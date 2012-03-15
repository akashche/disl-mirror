package ch.usi.dag.disl.example.sharing.runtime;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import ch.usi.dag.disl.example.shadowheap.runtime.ShadowObject;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public final class SharedFieldsShadowObject implements ShadowObject {

	private final String className;

	private final String allocationSite;

	private final WeakReference<Thread> allocatingThread;

	private final String[] shadowFieldIds;

	private final ShadowFieldState[] shadowFields;

	public static final char SUBSEP = '\034';

	public static final String ARRAYLENGTH_PSEUDO_FIELD_ID =
			"$arraylength" + SUBSEP + SharedFieldsShadowObject.asDescriptor(Integer.TYPE);

	public static final String COMPONENTS_PSEUDO_FIELD_ID =
			"$components" + SUBSEP + "?";

	public static final LoadingCache<Class<?>, Integer> NUMBER_OF_INSTANCE_FIELDS =
			CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>, Integer>() {

				@Override
				public Integer load(Class<?> clazz) throws Exception {
					return clazz.isArray() ? 2 : getNumberOfClassInstanceFields(clazz);
				}

				private Integer getNumberOfClassInstanceFields(Class<?> clazz) {
					int numberOfInstanceFields = 0;

					for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass())
						for (Field field : currentClass.getDeclaredFields())
							if (!Modifier.isStatic(field.getModifiers()))
								numberOfInstanceFields++;

					return numberOfInstanceFields;
				}
			});

	/**
	 * For each class, the associated array contains <em>two</em> Strings, the name of the class which first declared a
	 * field and that field's ID.
	 */
	public static final LoadingCache<Class<?>, String[]> CLASS_NAMES_AND_FIELD_IDS =
			CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>, String[]>() {

				@Override
				public String[] load(Class<?> clazz) throws Exception {
					return clazz.isArray() ? new String[] {
						clazz.getName(), ARRAYLENGTH_PSEUDO_FIELD_ID,
						clazz.getName(), COMPONENTS_PSEUDO_FIELD_ID
					} : getClassNamesAndFieldIds(clazz);
				}

				private String[] getClassNamesAndFieldIds(Class<?> clazz) {
					String[] classNamesAndfieldIds = new String[2 * NUMBER_OF_INSTANCE_FIELDS.getUnchecked(clazz)];
					int currentSlot = classNamesAndfieldIds.length - 1;

					for (Class<?> currentClass = clazz; currentClass != null; currentClass = currentClass.getSuperclass())
						for (Field field : currentClass.getDeclaredFields())
							if (!Modifier.isStatic(field.getModifiers())) {
								classNamesAndfieldIds[currentSlot--] = getFieldId(field);
								classNamesAndfieldIds[currentSlot--] = currentClass.getName();
							}

					return classNamesAndfieldIds;
				}
			});

	public SharedFieldsShadowObject(Object object, String allocationSite, Thread allocatingThread) {
		final Class<?> clazz = object.getClass();
		final int numberOfInstanceFields = NUMBER_OF_INSTANCE_FIELDS.getUnchecked(clazz);

		this.className = clazz.getName();
		this.allocationSite = allocationSite;
		this.allocatingThread = new WeakReference<Thread>(allocatingThread);
		this.shadowFieldIds = CLASS_NAMES_AND_FIELD_IDS.getUnchecked(clazz);
		this.shadowFields = new ShadowFieldState[numberOfInstanceFields];
		for (int i = 0; i < shadowFields.length; i++)
			shadowFields[i] = new ShadowFieldState();
	}

	@Override
	public void dump(PrintStream out) {
		out.append(allocationSite).append('\t');
		out.append(className).append('\n');

		for (int i = 0; i < shadowFields.length; i++) {
			out.append(shadowFieldIds[2 * i]).append(SUBSEP).append(shadowFieldIds[2 * i + 1]).append('\t');
			out.append(Integer.toString(shadowFields[i].getFieldReadsByAllocatingThread())).append('\t');
			out.append(Integer.toString(shadowFields[i].getFieldReadsByOtherThreads())).append('\t');
			out.append(Integer.toString(shadowFields[i].getFieldWritesByAllocatingThread())).append('\t');
			out.append(Integer.toString(shadowFields[i].getFieldWritesByOtherThreads())).append('\n');
		}

		out.append('\n');
	}

	/**
	 * Gets the canonical ID for a fields. All canonical field IDs are intern; they can be compared for identity.
	 * 
	 * @see ch.usi.dag.disl.example.sharing.instrument.FieldAccessStaticContext#getFieldId()
	 */
	private static String getFieldId(Field field) {
		return (field.getName() + SUBSEP + asDescriptor(field.getType())).intern();
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

	public void onFieldRead(Class<?> owner, String fieldId) {
		final int fieldIndex = resolveFieldIndex(owner, fieldId);

		if (fieldIndex < 0) {
			System.err.println("Warning: Not found " + owner + "." + fieldId);
			return;
		}

		ShadowFieldState readField = shadowFields[fieldIndex];

		if (Thread.currentThread() == allocatingThread.get())
			readField.onFieldReadByAllocatingThread();
		else
			readField.onFieldReadByOtherThread();
	}


	public void onFieldWrite(Class<?> owner, String fieldId) {
		final int fieldIndex = resolveFieldIndex(owner, fieldId);

		if (fieldIndex < 0) {
			System.err.println("Warning: Not found " + owner + "." + fieldId);
			return;
		}

		ShadowFieldState writtenField = shadowFields[fieldIndex];

		if (Thread.currentThread() == allocatingThread.get())
			writtenField.onFieldWriteByAllocatingThread();
		else
			writtenField.onFieldWriteByOtherThread();
	}

	private int resolveFieldIndex(Class<?> owner, String fieldId) {
		assert shadowFields.length == 2 * shadowFieldIds.length;

		int slotId = NUMBER_OF_INSTANCE_FIELDS.getUnchecked(owner);

		while (--slotId >= 0)
			if (shadowFieldIds[2 * slotId + 1] == fieldId)
				return slotId;

		return -1;
	}
}
