package ch.usi.dag.disl.example.racer;


import java.util.Stack;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.classcontext.ClassContext;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.racer.runtime.AdviceExecutor;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.BytecodeStaticContext;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;
public class DiSLClass {

	
	

	
	@ThreadLocal
	private static Stack<Object> bag;
	
	@Before(marker = BytecodeMarker.class, args = "monitorenter")
	public static void onMonitorEnter(MethodStaticContext sc, DynamicContext dc) {
		Object objectOnLock = dc.getStackValue(0, Object.class);
		if (bag == null)
			bag = new Stack<Object>();			bag.add(objectOnLock);
	}
	
	@Before(marker = BytecodeMarker.class, args = "monitorexit")
	public static void onMonitorExit(MethodStaticContext sc, DynamicContext dc) {
		if(bag == null) 
			System.err.println("MONITOREXIT CAN'T HAPPEN BEFORE MONITORENTER!!!");
		bag.pop();
	}
	
	@Before(marker = BytecodeMarker.class, args = "getfield")
	public static void beforeFieldRead(FieldAccessStaticContext sc, DynamicContext dc, MethodStaticContext msc) {
		if (bag == null)
			bag = new Stack<Object>();			Object fieldOwner = dc.getStackValue(0, Object.class);
		String fieldID = sc.getFieldId();
		String accessSite = sc.thisMethodFullName();
//		System.out.println("BFR: " + fieldID + "\t" + msc.thisMethodFullName());
		AdviceExecutor.getInstance().onFieldAccess(fieldOwner, fieldID, accessSite, bag, true);
	}

	@Before(marker = BytecodeMarker.class, args = "putfield")
	public static void beforeFieldWrite(FieldAccessStaticContext sc, DynamicContext dc, MethodStaticContext msc) {
		if (bag == null)
			bag = new Stack<Object>();			
		Object fieldOwner = dc.getStackValue(1, Object.class);
		String fieldID =  sc.getFieldId();
		String accessSite = sc.thisMethodFullName();

//		System.out.println("BFW: " + fieldID + "\t" + msc.thisMethodFullName());
		AdviceExecutor.getInstance().onFieldAccess(fieldOwner, fieldID, accessSite, bag, false);
	}
	
	
	
	@After(marker = BytecodeMarker.class, args = "getstatic")
	public static void beforeStaticFieldRead(FieldAccessStaticContext sc, DynamicContext dc, ClassContext cc) {
		if (bag == null)
			bag = new Stack<Object>();			
		Object clazz = cc.asClass(sc.getOwner());
		String accessSite = sc.thisMethodFullName();

		String fieldID = sc.getFieldId();
		AdviceExecutor.getInstance().onFieldAccess(clazz, fieldID, accessSite, bag, true);

	}

	@Before(marker = BytecodeMarker.class, args = "putstatic")
	public static void beforeStaticFieldWrite(FieldAccessStaticContext sc, DynamicContext dc, ClassContext cc) {
		if (bag == null)
			bag = new Stack<Object>();			
		Object clazz = cc.asClass(sc.getOwner());
		String fieldID = sc.getFieldId();
		String accessSite = sc.thisMethodFullName();

		AdviceExecutor.getInstance().onFieldAccess(clazz, fieldID, accessSite, bag, false);
		
	}
	public static class ConstructorGuard {
		@GuardMethod
		public static boolean isApplicable(MethodStaticContext sc) {
			return "<init>".equals(sc.thisMethodName());
		}
	}
	
}
