package ch.usi.dag.disl.util;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.DiSLException;

public class ClassFactory {

	public static Object createInstance(Type type) throws DiSLException {
		
		try {
			return resolve(type).newInstance();
		} catch (Exception e) {
			throw new DiSLException("Class " + type.getClassName()
					+ " cannot be instantiated", e);
		}
	}
	
	public static Class<?> resolve(Type type) throws DiSLException {
		try {
			return Class.forName(type.getClassName());
		} catch (ClassNotFoundException e) {
			throw new DiSLException("Class " + type.getClassName()
					+ " cannot be resolved", e);
		}
	}
}
