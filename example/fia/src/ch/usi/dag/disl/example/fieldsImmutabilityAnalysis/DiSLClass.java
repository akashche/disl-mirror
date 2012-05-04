package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import java.util.ArrayDeque;
import java.util.Deque;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.ImmutabilityAnalysis;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;

public class DiSLClass {
	@ThreadLocal
	private static Deque<Object> objectsUnderConstruction;

	/** STACK MAINTENANCE **/
	@Before(marker = BodyMarker.class, guard = OnlyInit.class)
	public static void beforeConstructor(DynamicContext dc) {
		try {
			if (objectsUnderConstruction == null)
				objectsUnderConstruction = new ArrayDeque<Object>();

			objectsUnderConstruction.push(dc.getThis());
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@After(marker = BodyMarker.class, guard = OnlyInit.class)
	public static void afterConstructor() {
		ImmutabilityAnalysis.instanceOf().popStackIfNonNull(objectsUnderConstruction);
	}

	/** ALLOCATION SITE **/
	@AfterReturning(marker = BytecodeMarker.class, args = "new")
	public static void afterReturningInit(MyMethodStaticContext sc, DynamicContext dc) {
		ImmutabilityAnalysis.instanceOf().onObjectInitialization(
				dc.getStackValue(0, Object.class), //the allocated object
				sc.getAllocationSite() //the allocation site
				);
	}

	/** FIELD ACCESSES **/
	@Before(marker = BytecodeMarker.class, args = "putfield")
	public static void beforeFieldWrite(MyMethodStaticContext sc, DynamicContext dc) {
		ImmutabilityAnalysis.instanceOf().onFieldWrite(
				dc.getStackValue(1, Object.class), //the accessed object
				sc.getFieldId(), //the field identifier
				objectsUnderConstruction //the stack of constructors
				);
	}

	@Before(marker = BytecodeMarker.class, args = "getfield")
	public static void afterFieldWrite(MyMethodStaticContext sc, DynamicContext dc) {
		ImmutabilityAnalysis.instanceOf().onFieldRead(
				dc.getStackValue(0, Object.class), //the accessed object
				sc.getFieldId(), //the field identifier
				objectsUnderConstruction //the stack of constructors
				);
	}
}
