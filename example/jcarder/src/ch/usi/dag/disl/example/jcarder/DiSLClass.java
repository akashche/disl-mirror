package ch.usi.dag.disl.example.jcarder;


import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.jcarder.runtime.JCarderAnalysis;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;




public class DiSLClass {

	

	@SyntheticLocal
	static boolean  lockOnThis = false;



	@Before(marker = BytecodeMarker.class, args = "monitorenter", guard = NotThread.class)
	public static void onMonitorEnter(MethodStaticContext sc, DynamicContext dc) {
		String mClassAndMethodName = sc.thisClassName() + "." + sc.thisMethodName()+ "()";
		if(dc.getStackValue(0, Object.class) == dc.getThis())
			lockOnThis = true;
		JCarderAnalysis.instanceOf().onMonitorEnter(dc.getStackValue(0, Object.class), mClassAndMethodName, lockOnThis);
	}

	@Before(marker = BytecodeMarker.class, args = "monitorexit", guard = NotThread.class)
	public static void onMonitorExit(MethodStaticContext sc, DynamicContext dc) {

		String mClassAndMethodName = sc.thisClassName() + "." + sc.thisMethodName()+ "()";
		if(dc.getStackValue(0, Object.class) == dc.getThis())
			lockOnThis = true;
		JCarderAnalysis.instanceOf().onMonitorExit(dc.getStackValue(0, Object.class), mClassAndMethodName, lockOnThis);

	}

}
