package ch.usi.dag.disl.example.classgen;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeCapability {
	
	static final Unsafe unsafe;

	static {
		try {
			// TODO The alternative is to put the analysis on the boot class path; not sure which one is worse.
			Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			unsafe = (sun.misc.Unsafe) field.get(null);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}
