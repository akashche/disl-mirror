package ch.usi.dag.disl.startutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.util.stack.StackUtil;

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

	private static void setTLVDefaultValues(ClassNode classNode,
			List<ThreadLocalVar> result) {

		MethodNode methodNode = null;

		for (MethodNode method : classNode.methods) {

			if (method.name.equals("<clinit>")) {
				methodNode = method;
				break;
			}
		}

		if (methodNode == null) {
			return;
		}

		Analyzer<SourceValue> analyzer = StackUtil.getSourceAnalyzer();

		try {
			analyzer.analyze(classNode.name, methodNode);
		} catch (AnalyzerException e) {
			return;
		}

		Frame<SourceValue>[] frames = analyzer.getFrames();

		for (ThreadLocalVar var : result) {

			for (int i = 0; i < frames.length; i++) {

				AbstractInsnNode instr = methodNode.instructions.get(i);

				if (instr.getOpcode() != Opcodes.PUTSTATIC) {
					continue;
				}

				FieldInsnNode fin = (FieldInsnNode) instr;

				if (!fin.name.equals(var.getName())) {
					continue;
				}

				Set<AbstractInsnNode> sources = frames[i].getStack(frames[i]
						.getStackSize() - 1).insns;

				if (sources.size() != 1) {
					continue;
				}

				AbstractInsnNode source = sources.iterator().next();

				switch (source.getOpcode()) {
				case Opcodes.ACONST_NULL:
					var.setDefaultValue("null");
					break;

				case Opcodes.ICONST_M1:
					var.setDefaultValue("-1");
					break;

				case Opcodes.ICONST_0:

					if (fin.desc.equals("Z")) {
						var.setDefaultValue("false");
					} else {
						var.setDefaultValue("0");
					}

					break;

				case Opcodes.LCONST_0:
					var.setDefaultValue("0");
					break;

				case Opcodes.FCONST_0:
				case Opcodes.DCONST_0:
					var.setDefaultValue("0.0");
					break;

				case Opcodes.ICONST_1:

					if (fin.desc.equals("Z")) {
						var.setDefaultValue("true");
					} else {
						var.setDefaultValue("1");
					}

					break;
				case Opcodes.LCONST_1:
					var.setDefaultValue("1");
					break;

				case Opcodes.FCONST_1:
				case Opcodes.DCONST_1:
					var.setDefaultValue("1.0");
					break;

				case Opcodes.ICONST_2:
				case Opcodes.FCONST_2:
					var.setDefaultValue("2");
					break;

				case Opcodes.ICONST_3:
					var.setDefaultValue("3");
					break;

				case Opcodes.ICONST_4:
					var.setDefaultValue("4");
					break;

				case Opcodes.ICONST_5:
					var.setDefaultValue("5");
					break;

				case Opcodes.BIPUSH:
				case Opcodes.SIPUSH:
					var.setDefaultValue("" + ((IntInsnNode) source).operand);
					break;

				case Opcodes.LDC:
					var.setDefaultValue(((LdcInsnNode) source).cst.toString());
					break;

				default:
					break;
				}

				break;
			}
		}
	}
}
