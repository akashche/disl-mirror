package ch.usi.dag.disl.example.map;

import java.util.Stack;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.staticcontext.MethodSC;
import ch.usi.dag.disl.example.map.runtime.MemoryRuntime;

// This is a DiSL implementation of the MAP tool

public class DiSLClass {
	
	@SyntheticLocal(initialize=Initialize.NEVER)
	public static Stack<Object> objref;
	 
	// capture 
/*
	@AfterReturning(marker = BodyMarker.class, scope = "java.lang.Object.<init>()")
	public static void afterObjectConstructor() {		
		MemoryRuntime.afterNewobject(null, "");
	//	int i = 1000;
	}
*/	
	
	
	@Before(marker = BytecodeMarker.class, args="getfield", scope = "*.*(...)")
	public static void beforeGet(DynamicContext di, MAPAnalysis ba) {		
		MemoryRuntime.beforeGetfield(di.stackValue(0, Object.class), ba.getFieldName());
	}
	
	
	/*
	@Before(marker = BytecodeMarker.class, param="putfield", scope = "*.*(...)")
	public static void beforePut(DynamicContext di, MyBytecodeAnalysis ba) {
		MemoryRuntime.beforePutfield(di.getStackValue(1, Object.class), ba.getFieldName());
	}
*/
	
	
	@Before(marker = BytecodeMarker.class, args="getstatic", scope = "*.*(...)")
	public static void beforeGetStatic(MAPAnalysis ba) {
		MemoryRuntime.beforeGetstatic(ba.getStaticFieldName());
	}
	
	@Before(marker = BytecodeMarker.class, args="putstatic", scope = "*.*(...)")
	public static void beforePutStatic(MAPAnalysis ba) {
		MemoryRuntime.beforePutstatic(ba.getStaticFieldName());
	}
	
	
	@Before(marker = BytecodeMarker.class, args="arraylength", scope = "*.*(...)")
	public static void beforeArrayLength(DynamicContext di) {
		MemoryRuntime.beforeArraylength(di.stackValue(0, Object.class));
	}
	
	// INIT THE SYNTHETIC LOCAL, only if a monitoenter is present in the body
	@Before(marker = BodyMarker.class, scope = "*.*", order = 0, guard = HasMonitorGuard.class) 
	public static void onMethodEnter(DynamicContext di, MethodSC sc) {
	//	System.out.println("INIT STACK " + sc.thisMethodFullName());
		objref = new Stack<Object>();
	}
	
	/*
	// .*oo() because it weaves also <clinit> => verification error... 
	@Before(marker = BodyMarker.class, scope = "*.*oo()", order = 0) 
	public static void onSynchronizedMethodEnter(DynamicContext di, StaticContext sc) {
		if(sc.isMethodSynchronized()) {  
				MemoryRuntime.afterMonitorenter(di.getLocalVariableValue(0, Object.class));
		}
	}
	
	// .*oo() because it weaves also <clinit> => verification error... 
	@AfterReturning(marker = BodyMarker.class, scope = "*.*oo()", order = 0)
	public static void onSynchronizedMethodExit(DynamicContext di, StaticContext sc) {
		if(sc.isMethodSynchronized()) { 
			    MemoryRuntime.beforeMonitorexit( di.getLocalVariableValue(0, Object.class));	
		}
	}
	*/
	
	
	// we are sure that the Stack objref was initialized thanks to the guard 
	@Before(marker = BytecodeMarker.class, args="monitorenter", scope = "*.*(...)") 
	public static void beforeMonitorEnter(DynamicContext di) {
		Object o = di.stackValue(0, Object.class);
		objref.push(o);
	}
	
	@After(marker = BytecodeMarker.class, args="monitorenter", scope = "*.*(...)")
	public static void afterMonitorEnter( ) {
		MemoryRuntime.afterMonitorenter(objref.pop());
	}
	
	@Before(marker = BytecodeMarker.class, args="monitorexit", scope = "*.*(...)") 
	public static void beforeMonitorExit(DynamicContext di) {
		MemoryRuntime.beforeMonitorexit(di.stackValue(0, Object.class));
	}
	
	
	

	@AfterReturning(marker = BytecodeMarker.class, args="newarray", scope = "*.*(...)")
	public static void afterNewArray(DynamicContext di) {
		MemoryRuntime.afterNewarray(di.stackValue(0, Object.class));
	}
	
	@AfterReturning(marker = BytecodeMarker.class, args="anewarray", scope = "*.*(...)")
	public static void afterANewArray(DynamicContext di) {
	    Object array = di.stackValue(0, Object.class);
		MemoryRuntime.afterAnewarray((Object[])array);
	}
	
	@AfterReturning(marker = BytecodeMarker.class, args="multianewarray", scope = "*.*(...)")
	public static void afterMultiANewArray(DynamicContext di,  MAPAnalysis ba) {
		MemoryRuntime.afterMultianewarray(di.stackValue(0, Object.class), 
				ba.getAMultiArrayDimension());
	}
	
	
	@Before(marker = BytecodeMarker.class, args="aaload", scope = "*.*(...)")
	public static void beforeAaLoad(DynamicContext di) {
		MemoryRuntime.beforeAaload((Object[]) di.stackValue(1, Object.class) ,
				 di.stackValue(0, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="aastore", scope = "*.*(...)")
	public static void beforeAaStore(DynamicContext di) {
		MemoryRuntime.beforeAastore((Object[]) di.stackValue(2, Object.class) ,
				 di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="saload", scope = "*.*(...)")
	public static void beforeSaload(DynamicContext di) {
		MemoryRuntime.beforeSaload( di.stackValue(1, short[].class),
				di.stackValue(0, int.class));
	}
	
	
	
	@Before(marker = BytecodeMarker.class, args="sastore", scope = "*.*(...)")
	public static void beforeSastore(DynamicContext di) {
		MemoryRuntime.beforeSastore(di.stackValue(2, short[].class),
				di.stackValue(1, int.class));
	}
	
	
	
	@Before(marker = BytecodeMarker.class, args="baload", scope = "*.*(...)")
	public static void beforeBaload(DynamicContext di) {
		
		MemoryRuntime.beforeBaload( di.stackValue(1, byte[].class),
				di.stackValue(0, int.class));
	}
	
	
	@Before(marker = BytecodeMarker.class, args="bastore", scope = "*.*(...)")
	public static void beforeBastore(DynamicContext di) {
		MemoryRuntime.beforeBastore(di.stackValue(2, byte[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="castore", scope = "*.*(...)")
	public static void beforeCastore(DynamicContext di) {
		MemoryRuntime.beforeCastore(di.stackValue(2, char[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="caload", scope = "*.*(...)")
	public static void beforeCaload(DynamicContext di) {
		MemoryRuntime.beforeCaload( di.stackValue(1, char[].class),
				di.stackValue(0, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="fastore", scope = "*.*(...)")
	public static void beforeFastore(DynamicContext di) {
		MemoryRuntime.beforeFastore(di.stackValue(2, float[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="faload", scope = "*.*(...)")
	public static void beforeFaload(DynamicContext di) {
		MemoryRuntime.beforeFaload( di.stackValue(1, float[].class),
				di.stackValue(0, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="dastore", scope = "*.*(...)")
	public static void beforeDastore(DynamicContext di) {
		MemoryRuntime.beforeDastore(di.stackValue(2, double[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="daload", scope = "*.*(...)")
	public static void beforeDaload(DynamicContext di) {
		MemoryRuntime.beforeDaload( di.stackValue(1, double[].class),
				di.stackValue(0, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="iastore", scope = "*.*(...)")
	public static void beforeIastore(DynamicContext di) {
		MemoryRuntime.beforeIastore(di.stackValue(2, int[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="iaload", scope = "*.*(...)")
	public static void beforeIaload(DynamicContext di) {
		MemoryRuntime.beforeIaload( di.stackValue(1, int[].class),
				di.stackValue(0, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="lastore", scope = "*.*(...)")
	public static void beforeLastore(DynamicContext di) {
		MemoryRuntime.beforeLastore(di.stackValue(2, long[].class),
				di.stackValue(1, int.class));
	}
	
	@Before(marker = BytecodeMarker.class, args="laload", scope = "*.*(...)")
	public static void beforeLaload(DynamicContext di) {
		MemoryRuntime.beforeLaload( di.stackValue(1, long[].class),
				di.stackValue(0, int.class));
	}
	
}
