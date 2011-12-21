package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Offsets {

	/**
	 * A mapping from registered classes to the canonical IDs of their respective instance fields (declared by the class or
	 * one of its superclasses).
	 */
	private static Map<Class<?>, String[]> canonicalFieldIDs = new WeakHashMap<Class<?>, String[]>();

	/**
	 * A mapping from field IDs to the respective field's offset. The field IDs need not be canonical.
	 */
	private static ConcurrentHashMap<String, Short> fieldOffsets = new ConcurrentHashMap<String, Short>();

	static {
		canonicalFieldIDs.put(null, new String[0]); // Register Object's "superclass" as a Null object.
	}

	public static void registerIfNeeded(Class<?> clazz) {
		synchronized (canonicalFieldIDs) {
			if (!isRegistered(clazz)) {
				registerIfNeeded(clazz.getSuperclass());
				registerCanonicalFieldIDs(clazz);
				registerFieldOffsets(clazz);
			}
		}
	}

	public static boolean isRegistered(Class<?> clazz) {
		synchronized (canonicalFieldIDs) {
			return canonicalFieldIDs.containsKey(clazz);
		}
	}

	public static int getNumberOfFields(Class<?> clazz) {
		synchronized (canonicalFieldIDs) {
			return canonicalFieldIDs.get(clazz).length;
		}
	}

	public static String[] getCanonicalFieldIDs(Class<?> clazz) {
		synchronized (canonicalFieldIDs) {
			return canonicalFieldIDs.get(clazz);
		}
	}

	public static String[] getFieldIDs(Class<?> clazz) {
		String[] canonicalFieldIDs = getCanonicalFieldIDs(clazz);
		String[] fieldIDs = new String[canonicalFieldIDs.length];
		for(int i = 0; i < canonicalFieldIDs.length; i++) {
			 fieldIDs[i] = clazz.getName() + ":" + canonicalFieldIDs[i].substring(canonicalFieldIDs[i].indexOf(':') + 1);
		}
		return fieldIDs;
	}

	public static Short getFieldOffset(String fieldId) {
		return fieldOffsets.get(fieldId);
	}

	private static void registerFieldOffsets(Class<?> clazz) {
		registerFieldOffsets(clazz, clazz);
	}

	private static void registerCanonicalFieldIDs(Class<?> clazz) {
		int numberOfNonStaticDeclaredFields = 0;
		for (Field field : clazz.getDeclaredFields())
			if (!Modifier.isStatic(field.getModifiers()))
				numberOfNonStaticDeclaredFields++;

		String[] canonicalFieldIDsOfSuperClass = canonicalFieldIDs.get(clazz.getSuperclass());
		String[] canonicalFieldIDsOfClass =
				Arrays.copyOf(canonicalFieldIDsOfSuperClass, canonicalFieldIDsOfSuperClass.length + numberOfNonStaticDeclaredFields);

		int currentOffset = canonicalFieldIDsOfSuperClass.length;
		for (Field field : clazz.getDeclaredFields())
			if (!Modifier.isStatic(field.getModifiers())){
				canonicalFieldIDsOfClass[currentOffset++] = getFieldId(field);
			}
		canonicalFieldIDs.put(clazz, canonicalFieldIDsOfClass);
	}

	/**
	 * Register the fields of {@code declaringClass} or one of its superclasses that are also accessible through {@code
	 * clazz} as well.
	 * 
	 * @return the number of fields that an instance of {@code clazz} has
	 */
	private static int registerFieldOffsets(Class<?> declaringClass, Class<?> clazz) {
		int numberOfFields = 0;

		if (declaringClass.getSuperclass() != null)
			numberOfFields += registerFieldOffsets(declaringClass.getSuperclass(), clazz);

		for (Field field : declaringClass.getDeclaredFields())
			if (!Modifier.isStatic(field.getModifiers()))
				fieldOffsets.put(getFieldId(clazz, field.getName(), field.getType()), (short) numberOfFields++);

		return numberOfFields;
	}

	public static String getFieldId(Field field) {
		return getFieldId(field.getDeclaringClass(), field.getName(), field.getType());
	}

	public static String getFieldId(Class<?> owner, String name, Class<?> type) {
		return asInternalName(owner) + ":" + name + ':' + asDescriptor(type);
	}

	private static String asInternalName(Class<?> clazz) {
		return clazz.getName().replace('.', '/');
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
			return type.getName().replace('.', '/');  // getName() already returns the name in type descriptor syntax
		} else {
			return "L" + type.getName().replace('.', '/') + ";";
		}
	}
}