package ch.usi.dag.disl.startutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

public class ClassParser {

	public static List<ThreadLocalVar> usedTLV(File dislClassFile)
			throws FileNotFoundException, IOException, ClassParserException {

		ClassReader cr = new ClassReader(new FileInputStream(dislClassFile));
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		// parse tlvs
		List<ThreadLocalVar> tlvs = 
			parseFields(classNode.name, classNode.fields);
		
		// set default values
		setTLVDefaultValues(classNode, tlvs);
		
		return tlvs;
	}

	private static List<ThreadLocalVar> parseFields(String name,
			List<FieldNode> fields) throws ClassParserException {

		List<ThreadLocalVar> result = new LinkedList<ThreadLocalVar>();
		
		for (FieldNode field : fields) {

			// skip unannotated fields
			if (field.invisibleAnnotations == null) {
				continue;
			}

			// skip fields with more then one annotation
			if (field.invisibleAnnotations.size() > 1) {
				continue;
			}

			AnnotationNode annotation = 
				(AnnotationNode) field.invisibleAnnotations.get(0);

			Type annotationType = Type.getType(annotation.desc);

			// skip, it it is not thread local
			if (! annotationType.equals(Type
					.getType(ch.usi.dag.disl.annotation.ThreadLocal.class))) {

				continue;
			}

			ThreadLocalVar tlv = parseThreadLocal(field, annotation);

			result.add(tlv);
		}

		return result;
	}

	private static ThreadLocalVar parseThreadLocal(FieldNode field,
			AnnotationNode annotation) throws ClassParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new ClassParserException("Field " + field.name
					+ " declared as ThreadLocal but is not static");
		}

		// default vals for init
		boolean inheritable = false;

		if (annotation.values != null) {

			Iterator<?> it = annotation.values.iterator();

			while (it.hasNext()) {

				String name = (String) it.next();

				if (name.equals("inheritable")) {

					inheritable = (Boolean) it.next();

					continue;
				}

				throw new RuntimeException("Unknow field " + name
						+ " in annotation at " + field.name
						+ ". This may happen if annotation class is changed"
						+ " but parser is not.");
			}
		}

		Type fieldType = Type.getType(field.desc);
		
		// default value will be set later on
		return new ThreadLocalVar(field.name, fieldType, inheritable);
	}
	
	private static Map<ThreadLocalVar, Object> setTLVDefaultValues(
			ClassNode classNode, List<ThreadLocalVar> result) {
		// TODO ! parse dafault value from static inti code
		return null;
	}
}
