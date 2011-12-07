package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import java.util.ArrayDeque;
import java.util.Deque;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.ImmutabilityAnalysis;
import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.OnlyInit;

import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {
	@ThreadLocal
	private static Deque<Object> stackTL;

	/** STACK MAINTENANCE **/
	@Before(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void before(DynamicContext dc, MethodStaticContext sc) {
		if(stackTL == null) {
			stackTL = new ArrayDeque<Object>();
		}

		stackTL.push(
				dc.thisValue() //the allocated object
			);
	}

	@After(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void after() {
		ImmutabilityAnalysis.instanceOf().popStackIfNonNull(stackTL);
	}

	/** ALLOCATION SITE **/
	@AfterReturning(marker = BytecodeMarker.class, args = "new", scope = "*.*", order = 0)
	public static void BeforeInitialization(MethodStaticContext sc, MyAnalysis ma, DynamicContext dc) {
		ImmutabilityAnalysis.instanceOf().onObjectInitialization(
				dc.stackValue(0, Object.class), //the allocated object
				ma.getAllocationSite() //the allocation site
			);
	}	

	/** FIELD ACCESSES **/
	@Before(marker=BytecodeMarker.class, args = "putfield", scope = "*.*",  order = 0)
	public static void onFieldWrite(MethodStaticContext sc, MyAnalysis ma, DynamicContext dc) {
		ImmutabilityAnalysis.instanceOf().onFieldWrite(
				dc.stackValue(1, Object.class), //the accessed object
				ma.getFieldId(), //the field identifier
				stackTL //the stack of constructors
			);
	}
}
