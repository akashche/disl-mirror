package ch.usi.dag.disl.example.sync.instrument;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.classcontext.ClassContext;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.sync.runtime.MonitorOwnershipAnalysis;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class MonitorOwnershipInstrumentation {

	@SyntheticLocal
	private static Object monitoredObject;

	/**
	 * Instruments the explicit request to acquire an object's monitor (<code>monitorenter</code>). This 
	 */
	@Before(marker = BytecodeMarker.class, args = "monitorenter", dynamicBypass = true)
	public static void explicitlyEnterMonitor(DynamicContext dc) {
		monitoredObject = dc.getStackValue(0, Object.class);
	}

	/**
	 * Instruments the <em>successful</em> (explicit) acquisition of an object's monitor (<code>monitorenter</code>).
	 */
	@AfterReturning(marker = BytecodeMarker.class, args = "monitorenter", dynamicBypass = true)
	public static void monitorEnteredExplictly(DynamicContext dc) {
		MonitorOwnershipAnalysis.getInstance().onMonitorEntry(monitoredObject);
	}

	/**
	 * Instruments the <em>successful</em> (implicit) acquisition of a monitor by entering a <code>synchronized</code> method.
	 * If the method is <code>static</code> the monitor associated with the respective instance of <code>java.lang.Class</code> 
	 * is acquired. Otherwise, the monitor associated with the receiver object is acquired.
	 */
	@Before(marker = BodyMarker.class, dynamicBypass = true, guard = SynchronizedMethodGuard.class)
	public static void monitorEnteredImplicitly(DynamicContext dc, MethodStaticContext sc, ClassContext cc) {
		Object associatedObject = sc.isMethodStatic() ?
				cc.asClass(sc.thisClassName()) : dc.getThis();
		MonitorOwnershipAnalysis.getInstance().onMonitorEntry(associatedObject);
	}

	/**
	 * Instruments the explicit request to release an object's monitor (<code>monitorexit</code>).
	 */
	@Before(marker = BytecodeMarker.class, args = "monitorexit", dynamicBypass = true)
	public static void explicitlyExitMonitor(DynamicContext dc) {
		MonitorOwnershipAnalysis.getInstance().onMonitorExit(dc.getStackValue(0, Object.class));
	}

	/**
	 * Instruments the implicit request to release a monitor of an instance of <code>java.lang.Class</code> by exiting a
	 * <code>synchronized</code> <code>static</code> method.
	 */
	@After(marker = BodyMarker.class, dynamicBypass = true, guard = SynchronizedMethodGuard.class)
	public static void implicitlyExitMonitor(DynamicContext dc, MethodStaticContext sc, ClassContext cc) {
		Object associatedObject = sc.isMethodStatic() ?
				cc.asClass(sc.thisClassName()) : dc.getThis();
		MonitorOwnershipAnalysis.getInstance().onMonitorExit(associatedObject);
	}

	public static final class SynchronizedMethodGuard {

		@GuardMethod
		public static boolean isApplicable(MethodStaticContext context) {
			return context.isMethodSynchronized();
		}
	}
}
