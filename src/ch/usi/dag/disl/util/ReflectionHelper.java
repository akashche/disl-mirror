package ch.usi.dag.disl.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.ReflectionException;

public class ReflectionHelper {

	/**
	 * Instantiates class using constructor with defined arguments.
	 * 
	 * @param classToInstantiate
	 * @param args
	 * @return
	 */
	public static Object createInstance(Class<?> classToInstantiate,
			Object... args) throws ReflectionException {

		try {

			// resolve constructor argument types
			Class<?>[] argTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; ++i) {
				argTypes[i] = args[i].getClass();
			}

			// resolve constructor
			Constructor<?> constructor = classToInstantiate
					.getConstructor(argTypes);

			// invoke constructor
			return constructor.newInstance(args);

		} catch (Exception e) {
			throw new ReflectionException("Class "
					+ classToInstantiate.getName() + " cannot be instantiated",
					e);
		}
	}

	private static String asmNameToJavaName(String asmClassName) {
		return asmClassName.replace('/', '.');
	}
	
	public static Class<?> resolveClass(Type asmType)
			throws ReflectionException {
		try {
			return Class.forName(asmNameToJavaName(asmType.getInternalName()));
		} catch (ClassNotFoundException e) {
			throw new ReflectionException("Class " + asmType.getClassName()
					+ " cannot be resolved", e);
		}
	}

	public static Method resolveMethod(Class<?> methodOwner, String methodName)
			throws ReflectionException {

		try {
			return methodOwner.getMethod(methodName);
		} catch (NoSuchMethodException e) {
			throw new ReflectionException("Method " + methodName + " in class "
					+ methodOwner.getName() + " cannot be found."
					+ " Snippet was probably compiled against a modified"
					+ " (different) class");
		}
	}
}
