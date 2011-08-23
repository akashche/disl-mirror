package ch.usi.dag.disl.dislclass.parser;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;

/**
 * Parses DiSL class with local variables
 */
public abstract class AbstractParser {

	protected LocalVars allLocalVars = new LocalVars();
	
	public LocalVars getAllLocalVars() {
		return allLocalVars;
	}
	
	// ****************************************
	// Local Variables Parsing and Processing
	// ****************************************
	
	// returns local vars defined in this class
	protected void processLocalVars(ClassNode classNode) throws ParserException {

		// parse local variables
		LocalVars localVars = parseLocalVars(classNode.name, classNode.fields);

		// add local vars from this class to all local vars from all classes
		allLocalVars.putAll(localVars);

		// get static initialization code
		InsnList origInitCodeIL = null;
		for (MethodNode method : classNode.methods) {

			// get the code
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				origInitCodeIL = method.instructions;
				break;
			}
		}

		// parse init code for synthetic local vars and assigns them accordingly
		if (origInitCodeIL != null) {
			parseInitCodeForSLV(origInitCodeIL, localVars.getSyntheticLocals());
		}
	}
	
	private LocalVars parseLocalVars(String className, List<FieldNode> fields)
			throws ParserException {

		// NOTE: if two synthetic local vars with the same name are defined
		// in different files they will be prefixed with class name as it is
		// also in byte code
		
		LocalVars result = new LocalVars();

		for (FieldNode field : fields) {

			if (field.invisibleAnnotations == null) {
				throw new ParserException("DiSL annotation for field "
						+ className + "." + field.name + " is missing");
			}

			if (field.invisibleAnnotations.size() > 1) {
				throw new ParserException("Field " + className + "."
						+ field.name + " may have only one anotation");
			}

			AnnotationNode annotation = 
				(AnnotationNode) field.invisibleAnnotations.get(0);

			Type annotationType = Type.getType(annotation.desc);

			// thread local
			if (annotationType.equals(Type.getType(
					ch.usi.dag.disl.dislclass.annotation.ThreadLocal.class))) {

				ThreadLocalVar tlv = parseThreadLocal(className, field,
						annotation);

				result.getThreadLocals().put(tlv.getID(), tlv);

				continue;
			}

			// synthetic local
			if (annotationType.equals(Type.getType(SyntheticLocal.class))) {

				SyntheticLocalVar slv = parseSyntheticLocal(className, field,
						annotation);

				result.getSyntheticLocals().put(slv.getID(), slv);

				continue;
			}

			throw new ParserException("Field " + className + "."
					+ field.name + " has unsupported DiSL annotation");
		}

		return result;
	}

	private ThreadLocalVar parseThreadLocal(String className, FieldNode field,
			AnnotationNode annotation) throws ParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new ParserException("Field " + className + "." + field.name
					+ " declared as ThreadLocal but is not static");
		}

		// here we can ignore annotation parsing - already done by start utility
		return new ThreadLocalVar(className, field.name);
	}

	private SyntheticLocalVar parseSyntheticLocal(String className,
			FieldNode field, AnnotationNode annotation)
			throws ParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new ParserException("Field " + field.name + className
					+ "." + " declared as SyntheticLocal but is not static");
		}

		// default val for init
		SyntheticLocal.Initialize slvInit = SyntheticLocal.Initialize.ALWAYS;

		if (annotation.values != null) {

			Iterator<?> it = annotation.values.iterator();

			while (it.hasNext()) {

				String name = (String) it.next();

				if (name.equals("initialize")) {

					// parse enum from string
					// first is class and second is enum value
					String[] slvInitStr = (String[]) it.next();
					slvInit = SyntheticLocal.Initialize.valueOf(slvInitStr[1]);

					continue;
				}

				throw new DiSLFatalException("Unknow field " + name
						+ " in annotation at " + className + "." + field.name
						+ ". This may happen if annotation class is changed"
						+ " but parser is not.");
			}
		}

		// field type
		Type fieldType = Type.getType(field.desc);
		
		return new SyntheticLocalVar(className, field.name, fieldType, slvInit);
	}
	
	// synthetic local var initialization can contain only basic constants
	// or single method calls
	private void parseInitCodeForSLV(InsnList origInitCodeIL,
			Map<String, SyntheticLocalVar> slVars) {

		// first initialization instruction for some field
		AbstractInsnNode firstInitInsn = origInitCodeIL.getFirst();

		for (AbstractInsnNode instr : origInitCodeIL.toArray()) {

			// if our instruction is field
			if (instr instanceof FieldInsnNode) {

				FieldInsnNode fieldInstr = (FieldInsnNode) instr;

				// get whole name of the field
				String wholeFieldName = fieldInstr.owner
						+ SyntheticLocalVar.NAME_DELIM + fieldInstr.name;

				SyntheticLocalVar slv = slVars.get(wholeFieldName);

				// something else then synthetic local var
				if (slv == null) {
					continue;
				}

				// clone part of the asm code
				InsnList initASMCode = simpleInsnListClone(origInitCodeIL,
						firstInitInsn, instr);

				// store the code
				slv.setInitASMCode(initASMCode);

				// prepare first init for next field
				firstInitInsn = instr.getNext();
			}

			// if opcode is return then we are done
			if (AsmHelper.isReturn(instr.getOpcode())) {
				break;
			}
		}
	}
	
	private InsnList simpleInsnListClone(InsnList src, AbstractInsnNode from,
			AbstractInsnNode to) {

		InsnList dst = new InsnList();

		// copy instructions using clone
		AbstractInsnNode instr = from;
		while (instr != to.getNext()) {

			// clone only real instructions - labels should not be needed
			if (!AsmHelper.isVirtualInstr(instr)) {

				dst.add(instr.clone(null));
			}

			instr = instr.getNext();
		}

		return dst;
	}
}
