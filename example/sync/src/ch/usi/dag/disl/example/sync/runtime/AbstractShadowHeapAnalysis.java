package ch.usi.dag.disl.example.sync.runtime;

import static com.google.common.cache.RemovalCause.COLLECTED;
import static com.google.common.cache.RemovalCause.EXPLICIT;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import ch.usi.dag.disl.dynamicbypass.DynamicBypass;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

public abstract class AbstractShadowHeapAnalysis<T extends ShadowObject> {

	private final String propertyPrefix;

	private final PrintStream out;

	private final LoadingCache<Object, T> shadowHeap;

	private static final int INITIAL_SHADOW_HEAP_CAPACITY = 2 << 16;

	private static final AbstractShadowHeapAnalysis<?> INSTANCE = new MonitorOwnershipAnalysis();

	public static final String UNKNOWN_ALLOCATION_SITE = "?";

	private static final Thread referenceHandlerThread = getReferenceHandlerThread();

	private static Thread getReferenceHandlerThread() {
		Set<Thread> threads = Thread.getAllStackTraces().keySet(); // Not very efficient, but simpler than the ThreadGroup API.

		for (Thread thread : threads)
			if ("Reference Handler".equals(thread.getName()))
				return thread;

		throw new RuntimeException("Thread named \"Reference Handler\" not found. Please make sure to use the HotSpot VM.");
	}

	protected static boolean isSaveToProcessEvent() {
		return Thread.currentThread() != referenceHandlerThread;
	}

	public static AbstractShadowHeapAnalysis<?> getInstance() {
		return INSTANCE;
	}

	public AbstractShadowHeapAnalysis() {
		propertyPrefix = getClass().getPackage().getName();
		out = openPrintStream();
		shadowHeap = CacheBuilder.newBuilder().weakKeys().removalListener(new RemovalListener<Object, T>() {

			@Override
			public void onRemoval(RemovalNotification<Object, T> entry) {
				assert entry.getCause() == COLLECTED || entry.getCause() == EXPLICIT;

				synchronized (out) {
					entry.getValue().dump(out);
				}
			}
		}).initialCapacity(INITIAL_SHADOW_HEAP_CAPACITY).concurrencyLevel(Runtime.getRuntime().availableProcessors()).build(new CacheLoader<Object, T>() {

			/**
			 * Handles the case where, for some reason, <code>object</code>'s allocation was not observed.
			 */
			@Override
			public T load(Object object) throws Exception {
				return createShadowObject(object, UNKNOWN_ALLOCATION_SITE);
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				DynamicBypass.activate(); // FIXME Should not be necessary. Remove once inheritance of bypass works.
				System.out.println("shutdown hook:" + DynamicBypass.isActivated());
				shadowHeap.invalidateAll(); // Force dumping of shadow heap.
				out.close();
			}
		}));
	}

	/**
	 * Creates a shadow object to be stored on the shadow heap.
	 * 
	 * @param object the object that is to be shadowed
	 * @param allocationSite the site where the object was allocated
	 * @return the created shadow object
	 */
	public abstract T createShadowObject(Object object, String allocationSite);

	public T getShadowObject(Object object)  {
		return shadowHeap.getUnchecked(object);
	}

	public String getPropertyPrefix() {
		return propertyPrefix;
	}

	private PrintStream openPrintStream() {
		try {
			final String dumpFile = System.getProperty(getPropertyPrefix() + ".Dump", "dump.tsv.gz");
			return new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(dumpFile))));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void onObjectAllocation(Object object, String allocationSite) {
		final T shadowObject = createShadowObject(object, allocationSite);
		final T previousValue = shadowHeap.asMap().put(object, shadowObject);

		assert previousValue == null;
	}

	/**
	 * Multiple arrays have been allocated in one go.
	 */
	public void onMultiArrayAllocation(Object array, String allocationSite) {
		if (array.getClass().getComponentType().isArray()) {
			final int dimensions = Array.getLength(array);
			for (int i = 0; i < dimensions; i++) {
				if (Array.get(array, i) != null)
					onMultiArrayAllocation(Array.get(array, i), allocationSite);
			}
		}

		onObjectAllocation(array, allocationSite);
	}
}
