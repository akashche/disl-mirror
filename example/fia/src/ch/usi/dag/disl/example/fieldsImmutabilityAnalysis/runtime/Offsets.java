package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Offsets {

	private static Map<Class<?>, Short> numberOfFields = new WeakHashMap<Class<?>, Short>();

	private static Map<String, Short> fieldOffsets = new ConcurrentHashMap<String, Short>();

	static {
		numberOfFields.put(null, (short) 0); // Register Object's "superclass" as a Null object.
	}

	public static void register(Class<?> clazz) {
		synchronized (numberOfFields) {
			if (!isRegistered(clazz.getSuperclass()))
				register(clazz.getSuperclass());

			numberOfFields.put(clazz, (short) registerFieldOffsets(clazz));
		}
	}

	public static short getNumberOfFields(Class<?> clazz) {
		return numberOfFields.get(clazz);
	}

	public static short getFieldOffset(String fieldId) {
		return fieldOffsets.get(fieldId);
	}

	public static String getFieldId(Class<?> owner, String name, Class<?> type) {
		return owner.getName().replace('.', '/') + ":" + name + ':' + asDescriptor(type);
	}

	public static boolean isRegistered(Class<?> clazz) {
		synchronized (numberOfFields) {
			return numberOfFields.containsKey(clazz);
		}
	}

	/**
	 * @return the number of fields that an instance of {@code clazz} has
	 */
	private static short registerFieldOffsets(Class<?> clazz) {
		return registerFieldOffsets(clazz, clazz);
	}

	/**
	 * Register the fields of {@code declaringClass} or one of its superclasses that are also accessible through {@code
	 * clazz} as well.
	 * 
	 * @return the number of fields that an instance of {@code clazz} has
	 */
	private static short registerFieldOffsets(Class<?> declaringClass, Class<?> clazz) {
		short numberOfFields = 0;

		if (declaringClass.getSuperclass() != null)
			numberOfFields += registerFieldOffsets(declaringClass.getSuperclass(), clazz);

		for (Field field : declaringClass.getDeclaredFields())
			if (!Modifier.isStatic(field.getModifiers()))
				fieldOffsets.put(getFieldId(clazz, field.getName(), field.getType()), numberOfFields++);

		return numberOfFields;
	}

	private static String asDescriptor(Class<?> type) throws AssertionError {
		if (type.isPrimitive()) {
			if (type == Integer.TYPE)
				return "I";
			else if (type == Long.TYPE)
				return "J";
			else if (type == Float.TYPE)
				return "F";
			else if (type == Double.TYPE)
				return "D";
			else if (type == Character.TYPE)
				return "C";
			else if (type == Boolean.TYPE)
				return "Z";
			else if (type == Byte.TYPE)
				return "B";
			else if (type == Short.TYPE)
				return "S";
			else
				throw new AssertionError("Unknown primitive: " + type);
		} else if (type.isArray()) {
			return type.getName(); // getName() already returns type descriptor syntax
		} else {
			return "L" + type.getName().replace('.', '/') + ";";
		}
	}
}
