package ch.usi.dag.disl.util;

import java.lang.reflect.Constructor;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.DiSLException;

public class ClassFactory {

	/**
	 * Instantiates class using constructor with defined arguments similarly
	 * to createInstance but returns null instead of exception if the
	 * instantiation was not successful.
	 * 
	 * The only exception when exception is thrown is when the constructor
	 * of the created instance throws DiSLException
	 * 
	 * @param classToInstantiate
	 * @param args
	 * @return
	 * @throws DiSLException
	 */
	public static Object tryCreateInstance(Class<?> classToInstantiate,
			Object... args) throws DiSLException {
		
		try {
			return createInstance(classToInstantiate, args);
		} catch (DiSLException e) {

			// constructor of the created instance throws DiSLException
			// exception -> propagate it
			if(e.getCause() != null 
					&& e.getCause().getCause() instanceof DiSLException) {
				
				throw new DiSLException("Marker error: ",
						e.getCause().getCause());
			}
			
			return null;
		}
		
	}
	
	/**
	 * Instantiates class using constructor with defined arguments.
	 * 
	 * @param classToInstantiate
	 * @param args
	 * @return
	 * @throws DiSLException
	 */
	public static Object createInstance(Class<?> classToInstantiate,
			Object... args) throws DiSLException {
		
		try {
			
			// resolve constructor argument types
			Class<?>[] argTypes = new Class<?>[args.length];
			for(int i = 0; i < args.length; ++i) {
				argTypes[i] = args[i].getClass();
			}
			
			// resolve constructor
			Constructor<?> constructor =
				classToInstantiate.getConstructor(argTypes);
			
			// invoke constructor
			return constructor.newInstance(args);
			
		} catch (Exception e) {
			throw new DiSLException("Class " + classToInstantiate.getName()
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
