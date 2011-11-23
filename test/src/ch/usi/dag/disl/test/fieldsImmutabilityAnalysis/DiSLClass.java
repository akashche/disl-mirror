package ch.usi.dag.disl.test.fieldsImmutabilityAnalysis;

import java.util.Stack;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.MethodSC;
import ch.usi.dag.disl.test.fieldsImmutabilityAnalysis.runtime.ImmutabilityAnalysis;


public class DiSLClass {

	@ThreadLocal
	static Stack<Object> stackTL;
	


	/** STACK MAINTENANCE **/
	@Before(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void before(DynamicContext dc, MethodSC sc) {
		if(stackTL == null) {
			stackTL = new Stack<Object>();
		}
		Object alloc = dc.thisValue();
		stackTL.push(alloc);
		
	}

	@After(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void after() {
		ImmutabilityAnalysis.afterConstructor(stackTL);

	}
	
	/** ALLOCATION SITE **/
	@AfterReturning(marker = BytecodeMarker.class, args = "new", scope = "*.*", order = 0)
	public static void BeforeInitialization(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		Object allocatedObj = dc.stackValue(0, Object.class);
		String allocationSite = sc.thisMethodFullName() + " [" + ma.getInMethodIndex() + "]";
		ImmutabilityAnalysis.onObjectInitialization(allocatedObj, allocationSite);
	}	
	
	
	

	/** FIELD ACCESSES **/
	@Before(marker=BytecodeMarker.class, args = "putfield", scope = "*.*", order = 0)
	public static void onFieldWrite(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		String accessSite = sc.thisMethodFullName() + " [" + ma.getInMethodIndex() + "]";
		Object accessedObj = dc.stackValue(1, Object.class);
		String accessedFieldName = ma.getAccessedFieldsName();

		ImmutabilityAnalysis.onFieldWrite(accessedObj, accessedFieldName, accessSite, stackTL);
	}

	@Before(marker=BytecodeMarker.class, args = "getfield", scope = "*.*", order = 0)
	public static void onFieldRead(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		String accessSite = sc.thisMethodFullName() + " [" + ma.getInMethodIndex() + "]";
		Object accessedObj = dc.stackValue(0, Object.class);
		String accessedFieldName = ma.getAccessedFieldsName();

		ImmutabilityAnalysis.onFieldRead(accessedObj, accessedFieldName, accessSite);
	}
}	