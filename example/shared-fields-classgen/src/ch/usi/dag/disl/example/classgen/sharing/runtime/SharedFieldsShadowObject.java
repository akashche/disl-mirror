package ch.usi.dag.disl.example.classgen.sharing.runtime;

import java.io.PrintStream;
import java.lang.ref.WeakReference;

import ch.usi.dag.disl.example.classgen.SyntheticIntegerFieldsUpdater;
import ch.usi.dag.disl.example.classgen.shadowheap.runtime.ShadowObject;

public class SharedFieldsShadowObject implements ShadowObject {

	public String className;

	public String allocationSite;

	public WeakReference<Thread> allocatingThread;

	public static final SyntheticIntegerFieldsUpdater<SharedFieldsShadowObject> READS_BY_ALLOCATING_THREAD_UPDATER =
			SyntheticIntegerFieldsUpdater.newUpdater(SharedFieldsShadowObject.class, "readsByAllocatingThread");

	public static final SyntheticIntegerFieldsUpdater<SharedFieldsShadowObject> READS_BY_OTHER_THREAD_UPDATER =
			SyntheticIntegerFieldsUpdater.newUpdater(SharedFieldsShadowObject.class, "readsByOtherThread");

	public static final SyntheticIntegerFieldsUpdater<SharedFieldsShadowObject> WRITES_BY_ALLOCATING_THREAD_UPDATER =
			SyntheticIntegerFieldsUpdater.newUpdater(SharedFieldsShadowObject.class, "writesByAllocatingThread");

	public static final SyntheticIntegerFieldsUpdater<SharedFieldsShadowObject> WRITES_BY_OTHER_THREAD_UPDATER =
			SyntheticIntegerFieldsUpdater.newUpdater(SharedFieldsShadowObject.class, "writesByOtherThread");

	public void onFieldRead(Class<?> owner, String fieldId) {
		if (Thread.currentThread() == allocatingThread.get())
			READS_BY_ALLOCATING_THREAD_UPDATER.increment(this, fieldId);
		else
			READS_BY_OTHER_THREAD_UPDATER.increment(this, fieldId);
	}

	public void onFieldWrite(Class<?> owner, String fieldId) {
		if (Thread.currentThread() == allocatingThread.get())
			WRITES_BY_ALLOCATING_THREAD_UPDATER.increment(this, fieldId);
		else
			WRITES_BY_OTHER_THREAD_UPDATER.increment(this, fieldId);
	}

	@Override
	public void dump(PrintStream out) {
		out.append(allocationSite).append('\t');
		out.append(className).append('\n');

		for (String fieldId : READS_BY_ALLOCATING_THREAD_UPDATER.getGenuineFieldIds(this)) {
			out.append(fieldId).append('\t');
			out.append(Integer.toString(READS_BY_ALLOCATING_THREAD_UPDATER.get(this, fieldId))).append('\t');
			out.append(Integer.toString(READS_BY_OTHER_THREAD_UPDATER.get(this, fieldId))).append('\t');
			out.append(Integer.toString(WRITES_BY_ALLOCATING_THREAD_UPDATER.get(this, fieldId))).append('\t');
			out.append(Integer.toString(WRITES_BY_OTHER_THREAD_UPDATER.get(this, fieldId))).append('\n');
		}

		out.append('\n');
	}
}
