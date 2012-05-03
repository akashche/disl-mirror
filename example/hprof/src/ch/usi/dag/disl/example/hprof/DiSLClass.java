package ch.usi.dag.disl.example.hprof;

import java.util.Stack;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.hprof.runtime.HPROFAnalysis;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.marker.NewObjMarker;
import ch.usi.dag.disl.staticcontext.uid.UniqueMethodId;

public class DiSLClass {
	@ThreadLocal
	public static Stack<Object> objStack;

	@AfterReturning(marker = BytecodeMarker.class, args = "newarray, anewarray, multianewarray")
	public static void eee(DynamicContext dc, MyMethodStaticContext sc, UniqueMethodId id) {
		Object allocatedObj = dc.getStackValue(0, Object.class);

        HPROFAnalysis.instanceOf().onObjectInitialization(
				allocatedObj, //the allocated object
				sc.getExtendedID(), 
				System.identityHashCode(allocatedObj.getClass().getClassLoader())
			);
	}

	@AfterReturning(marker = BytecodeMarker.class, args="new")
	public static void fff(DynamicContext dc) {
		if(objStack == null) objStack = new Stack<Object>();
		objStack.push(dc.getStackValue(0, Object.class));
	}

	/** ALLOCATION SITE **/
	@AfterReturning(marker = NewObjMarker.class,  order = 10)
	public static void afterReturningNew(MyMethodStaticContext sc, DynamicContext dc, UniqueMethodId id) {

			if(!objStack.empty()) {
				Object allocatedObj = objStack.pop();
				HPROFAnalysis.instanceOf().onObjectInitialization(
						allocatedObj, //the allocated object
						sc.getExtendedID(), 
						System.identityHashCode(allocatedObj.getClass().getClassLoader())
					);
			} else {
				System.out.println("EMPTY STACK\t" + Thread.currentThread().getName() +"\t" + sc.thisMethodFullName());
			}
	}

}
