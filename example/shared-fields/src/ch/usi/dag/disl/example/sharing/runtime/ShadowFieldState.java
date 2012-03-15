package ch.usi.dag.disl.example.sharing.runtime;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

class ShadowFieldState {

	private volatile int fieldReadsByAllocatingThread = 0;

	private volatile int fieldReadsByOtherThreads = 0;

	private volatile int fieldWritesByAllocatingThread = 0;

	private volatile int fieldWritesByOtherThreads = 0;

	private static final AtomicIntegerFieldUpdater<ShadowFieldState> FIELD_READS_BY_ALLOCATING_THREAD =
			AtomicIntegerFieldUpdater.newUpdater(ShadowFieldState.class, "fieldReadsByAllocatingThread");

	private static final AtomicIntegerFieldUpdater<ShadowFieldState> FIELD_READS_BY_OTHER_THREADS =
			AtomicIntegerFieldUpdater.newUpdater(ShadowFieldState.class, "fieldReadsByOtherThreads");

	private static final AtomicIntegerFieldUpdater<ShadowFieldState> FIELD_WRITES_BY_ALLOCATING_THREAD =
			AtomicIntegerFieldUpdater.newUpdater(ShadowFieldState.class, "fieldWritesByAllocatingThread");

	private static final AtomicIntegerFieldUpdater<ShadowFieldState> FIELD_WRITES_BY_OTHER_THREADS =
			AtomicIntegerFieldUpdater.newUpdater(ShadowFieldState.class, "fieldWritesByOtherThreads");

	public void onFieldReadByAllocatingThread() {
		FIELD_READS_BY_ALLOCATING_THREAD.incrementAndGet(this);
	}

	public void onFieldReadByOtherThread() {
		FIELD_READS_BY_OTHER_THREADS.incrementAndGet(this);
	}

	public void onFieldWriteByAllocatingThread() {
		FIELD_WRITES_BY_ALLOCATING_THREAD.incrementAndGet(this);
	}

	public void onFieldWriteByOtherThread() {
		FIELD_WRITES_BY_OTHER_THREADS.incrementAndGet(this);
	}

	public int getFieldReadsByAllocatingThread() {
		return fieldReadsByAllocatingThread;
	}

	public int getFieldReadsByOtherThreads() {
		return fieldReadsByOtherThreads;
	}

	public int getFieldWritesByAllocatingThread() {
		return fieldWritesByAllocatingThread;
	}

	public int getFieldWritesByOtherThreads() {
		return fieldWritesByOtherThreads;
	}
}