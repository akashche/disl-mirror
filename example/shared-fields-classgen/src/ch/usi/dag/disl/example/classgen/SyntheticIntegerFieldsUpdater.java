package ch.usi.dag.disl.example.classgen;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;

public class SyntheticIntegerFieldsUpdater<T> extends SyntheticFieldsUpdater<T> {

	// TODO The outer cache can actually be removed, if each generated shadow class contains its own inner cache.
	private final LoadingCache<Class<? extends T>, LoadingCache<String, AtomicIntegerFieldUpdater<? extends T>>> atomicUpdaters =
			CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<? extends T>, LoadingCache<String, AtomicIntegerFieldUpdater<? extends T>>>() {

				@Override
				public LoadingCache<String, AtomicIntegerFieldUpdater<? extends T>> load(final Class<? extends T> templateInstance) throws Exception {
					return CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicIntegerFieldUpdater<? extends T>>() {

						@Override
						public AtomicIntegerFieldUpdater<? extends T> load(String canonicalFieldId) throws Exception {
							final String syntheticFieldName = getSyntheticFieldName(canonicalFieldId);
							return AtomicIntegerFieldUpdater.newUpdater(templateInstance, syntheticFieldName);
						}
					});
				}
			});

	public static final char SUBSEP = '\034';

	private static final ConcurrentMap<Class<?>, Set<SyntheticIntegerFieldsUpdater<?>>> REGISTERED_UPDATERS =
			new MapMaker().weakKeys().makeMap();

	public SyntheticIntegerFieldsUpdater(String prefix) {
		super(prefix);
	}

	public static <U> SyntheticIntegerFieldsUpdater<U> newUpdater(Class<U> templateClass, String prefix) {
		SyntheticIntegerFieldsUpdater<U> updater = new SyntheticIntegerFieldsUpdater<U>(prefix);

		if (REGISTERED_UPDATERS.containsKey(templateClass))
			REGISTERED_UPDATERS.get(templateClass).add(updater);
		else
			REGISTERED_UPDATERS.put(templateClass, new HashSet<SyntheticIntegerFieldsUpdater<?>>(Collections.singleton(updater)));

		return updater;
	}

	public static Set<SyntheticIntegerFieldsUpdater<?>> getUpdaters(Class<?> templateClass) {
		return REGISTERED_UPDATERS.get(templateClass);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void increment(T object, String canonicalFieldId) {
		AtomicIntegerFieldUpdater updater = getAtomicFieldUpdater((Class<? extends T>) object.getClass(), canonicalFieldId);
		updater.incrementAndGet(object);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int get(T object, String canonicalFieldId) {
		AtomicIntegerFieldUpdater updater = getAtomicFieldUpdater((Class<? extends T>) object.getClass(), canonicalFieldId);
		return updater.get(object);
	}

	private AtomicIntegerFieldUpdater<? extends T> getAtomicFieldUpdater(Class<? extends T> templateInstance, String canonicalFieldId) {
		return atomicUpdaters.getUnchecked(templateInstance).getUnchecked(canonicalFieldId);
	}
}
