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
import ch.usi.dag.disl.staticcontext.MethodSC;


public class DiSLClass {

	@ThreadLocal
	private static Deque<Object> stackTL;
	
	/** STACK MAINTENANCE **/
	@Before(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void before(DynamicContext dc, MethodSC sc) {
		if(stackTL == null) {
			stackTL = new ArrayDeque<Object>();
		}
		Object alloc = dc.thisValue();
		stackTL.push(alloc);
	}

	@After(marker = BodyMarker.class, scope = "*.*", guard = OnlyInit.class, order = 1)
	public static void after() {
		ImmutabilityAnalysis.popStackIfNonNull(stackTL);
	}

	/** ALLOCATION SITE **/
	@AfterReturning(marker = BytecodeMarker.class, args = "new", guard = NoClInit.class, scope = "*.*", order = 0)
	public static void BeforeInitialization(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		Object allocatedObj = dc.stackValue(0, Object.class);
		String allocationSite = sc.thisMethodFullName() + " [" + ma.getInMethodIndex() + "]";
		ImmutabilityAnalysis.onObjectInitialization(allocatedObj, allocationSite);
	}	
	
	

	/** FIELD ACCESSES **/
	@Before(marker=BytecodeMarker.class, args = "putfield", guard = NoClInit.class, scope = "*.*",  order = 0)
	public static void onFieldWrite(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		Object accessedObj = dc.stackValue(1, Object.class);
		String accessedFieldName = ma.getAccessedFieldsName();
		ImmutabilityAnalysis.onFieldWrite(accessedObj, accessedFieldName, stackTL);
	}

	@Before(marker=BytecodeMarker.class, args = "getfield", guard = NoClInit.class, scope = "*.*", order = 0)
	public static void onFieldRead(MethodSC sc, MyAnalysis ma, DynamicContext dc) {
		Object accessedObj = dc.stackValue(0, Object.class);
		String accessedFieldName = ma.getAccessedFieldsName();
		ImmutabilityAnalysis.onFieldRead(accessedObj, accessedFieldName);
	}
}	
