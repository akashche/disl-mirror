package ch.usi.dag.disl.example.classgen;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

public class SyntheticFieldsUpdater<T> {

	private final String prefix;

	private static final char SUBSEP = '\034';

	public SyntheticFieldsUpdater(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}

	// TODO Cache this
	public List<String> getGenuineFieldIds(T object) {
		List<String> fieldIds = new ArrayList<String>();

		for (Field field : object.getClass().getDeclaredFields())
			if (field.getName().startsWith(prefix))
				fieldIds.add(field.getName().substring(prefix.length() + 1));

		return fieldIds;
	}

	public String getSyntheticFieldName(String fieldId) {
		return prefix + SUBSEP + fieldId.replace('[', '$'); // Apparently, HotSpot disallows '['s in field names. 
	}

	public String getSyntheticFieldName(Field field) {
		// Apparently, HotSpot disallows '['s in field names.
		return prefix + SUBSEP + field.getName() + SUBSEP + Type.getDescriptor(field.getType()).replace('[', '$');
	}
}
