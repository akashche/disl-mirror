package ch.usi.dag.disl.example.classgen;

import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.MapMaker;

public class ShadowClassDefiner<T> {

	private final Class<T> templateClass;

	private final ConcurrentMap<Class<?>, Class<? extends T>> shadowClasses =
			new MapMaker().weakKeys().makeMap();

	private static final String SHADOW_CLASS_PREFIX = "ch/usi/dag/disl/ShadowClass$";

	public ShadowClassDefiner(Class<T> templateClass) {
		this.templateClass = templateClass;
		UnsafeCapability.unsafe.ensureClassInitialized(templateClass);
	}

	@SuppressWarnings("unchecked")
	public Class<? extends T> getShadowClass(Class<?> shadowedClass) {
		Class<? extends T> shadowClass;

		if ((shadowClass = shadowClasses.get(shadowedClass)) == null) {
			final String shadowClassName = SHADOW_CLASS_PREFIX + shadowedClass.getName().replace('.', '_');
			final ShadowClassGenerator generator = new ShadowClassGenerator(shadowClassName, shadowedClass, templateClass);
			final byte[] classBytes = generator.generate();
			shadowClass = UnsafeCapability.unsafe.defineClass(shadowClassName, classBytes, 0, classBytes.length);
			shadowClasses.put(shadowedClass, shadowClass);
		}

		return shadowClass;
	}
}
