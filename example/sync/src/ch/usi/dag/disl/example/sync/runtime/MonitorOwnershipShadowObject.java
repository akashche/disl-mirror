package ch.usi.dag.disl.example.sync.runtime;

import java.io.PrintStream;
import java.lang.ref.WeakReference;

import ch.usi.dag.disl.example.sync.runtime.ShadowObject;

/**
 * @author Andreas Sewe
 */
public class MonitorOwnershipShadowObject implements ShadowObject {

	private final String className;

	private final String allocationSite;

	/**
	 * The field is either <code>null</null>, indicating that the object's monitor has not yet been acquired,
	 * a reference to the only thread that acquired the object's monitor so far, or <code>MULTIPLE_OWNERS</code>,
	 * indicating that multiple threads have acquired this object's monitor.
	 * 
	 * Note that this is a linear progression of states.
	 */
	private WeakReference<Thread> owners = null;

	private int monitorAcquisitions = 0;

	/**
	 * We don't expect more than 2^15 recursive monitor entries on the same object; the call stack will be exceeded earlier
	 * than that.
	 */
	private short currentRecursiveCount = 0;

	/**
	 * We don't expect more than 2^15 recursive monitor entries on the same object; the call stack will be exceeded earlier
	 * than that.
	 */
	private short maxRecursiveCount = 0;

	private static final WeakReference<Thread> MULTIPLE_OWNERS = new WeakReference<Thread>(null);

	public MonitorOwnershipShadowObject(Object object, String allocationSite) {
		className = object.getClass().getName();
		this.allocationSite = allocationSite;
	}

	public synchronized void onMonitorEntry() {
		if (owners == null) owners = new WeakReference<Thread>(Thread.currentThread());

		if (owners.get() != Thread.currentThread()) owners = MULTIPLE_OWNERS;

		monitorAcquisitions++;
		currentRecursiveCount++;

		if (currentRecursiveCount > maxRecursiveCount)
			maxRecursiveCount = currentRecursiveCount;
	}

	public synchronized void onMonitorExit() {
		currentRecursiveCount--;
	}

	@Override
	public void dump(PrintStream out) {
		out.print(allocationSite);
		out.print('\t');
		out.print(className);
		out.print('\t');
		out.print(monitorAcquisitions);
		out.print('\t');
		out.print(maxRecursiveCount);
		out.print('\t');
		out.print(owners != MULTIPLE_OWNERS);
		out.println();
	}
}
