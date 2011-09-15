package ch.usi.dag.disl.dislclass.parser;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.stack.StackUtil;
import ch.usi.dag.jborat.tools.thread.ExtendThread;

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
		MethodNode cinit = null;
		for (MethodNode method : classNode.methods) {

			// get the code
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				cinit = method;
				break;
			}
		}

		// parse init code for synthetic local vars and assigns them accordingly
		if (cinit.instructions != null) {
			parseInitCodeForSLV(cinit.instructions, localVars.getSyntheticLocals());
			parseInitCodeForTLV(classNode.name, cinit, localVars.getThreadLocals());
		}
		
		for(ThreadLocalVar tlv : localVars.getThreadLocals().values()) {
			ExtendThread.addField(tlv.getName(), tlv.getTypeAsDesc(),
					tlv.getDefaultValue(), tlv.isInheritable());
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

	private static class TLAnnotationData {
		
		public boolean inheritable = false; // default
	}
	private ThreadLocalVar parseThreadLocal(String className, FieldNode field,
			AnnotationNode annotation) throws ParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new ParserException("Field " + className + "." + field.name
					+ " declared as ThreadLocal but is not static");
		}
		
		// parse annotation
		TLAnnotationData tlad = new TLAnnotationData();
		ParserHelper.parseAnnotation(tlad, annotation);

		Type fieldType = Type.getType(field.desc);
		
		// default value will be set later on
		return new ThreadLocalVar(className, field.name, fieldType,
				tlad.inheritable);
	}

	private static class SLAnnotaionData {
		
		// see code below for default
		public String[] initialize = null;
	}
	
	private SyntheticLocalVar parseSyntheticLocal(String className,
			FieldNode field, AnnotationNode annotation)
			throws ParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new ParserException("Field " + field.name + className
					+ "." + " declared as SyntheticLocal but is not static");
		}

		// parse annotation data
		SLAnnotaionData slad = new SLAnnotaionData();
		ParserHelper.parseAnnotation(slad, annotation);
		
		// default val for init
		SyntheticLocal.Initialize slvInit = SyntheticLocal.Initialize.ALWAYS;
		
		if(slad.initialize != null) {

			// enum is converted to array
			//  - first value is class name
			//  - second value is value name
			slvInit = SyntheticLocal.Initialize.valueOf(slad.initialize[1]);
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
	
	private void parseInitCodeForTLV(String className, MethodNode cinitMethod,
			Map<String, ThreadLocalVar> tlvs) throws ParserException {

		// crate analyzer
		Analyzer<SourceValue> analyzer = StackUtil.getSourceAnalyzer();

		try {
			analyzer.analyze(className, cinitMethod);
		} catch (AnalyzerException e) {
			throw new ParserException(e);
		}

		Frame<SourceValue>[] frames = analyzer.getFrames();

		// analyze instructions in each frame
		// one frame should cover one field initialization
		for (int i = 0; i < frames.length; i++) {

			AbstractInsnNode instr = cinitMethod.instructions.get(i);

			// if the last instruction puts some value into the field...
			if (instr.getOpcode() != Opcodes.PUTSTATIC) {
				continue;
			}

			FieldInsnNode fin = (FieldInsnNode) instr;

			ThreadLocalVar tlv = 
				tlvs.get(className + ThreadLocalVar.NAME_DELIM + fin.name);
			
			// ... and the field is thread local var
			if (tlv == null) {
				continue;
			}

			// get the instruction that put the field value on the stack
			Set<AbstractInsnNode> sources = frames[i].getStack(frames[i]
					.getStackSize() - 1).insns;

			if (sources.size() != 1) {
				throw new ParserException("Thread local variable "
						+ tlv.getName()
						+ " can be initialized only by single constant");
			}

			AbstractInsnNode source = sources.iterator().next();

			// analyze oppcode and set the proper default value
			switch (source.getOpcode()) {
			// not supported
			// case Opcodes.ACONST_NULL:
			// var.setDefaultValue(null);
			// break;

			case Opcodes.ICONST_M1:
				tlv.setDefaultValue(-1);
				break;

			case Opcodes.ICONST_0:

				if (fin.desc.equals("Z")) {
					tlv.setDefaultValue(false);
				} else {
					tlv.setDefaultValue(0);
				}

				break;

			case Opcodes.LCONST_0:
				tlv.setDefaultValue(0);
				break;

			case Opcodes.FCONST_0:
			case Opcodes.DCONST_0:
				tlv.setDefaultValue(0.0);
				break;

			case Opcodes.ICONST_1:

				if (fin.desc.equals("Z")) {
					tlv.setDefaultValue(true);
				} else {
					tlv.setDefaultValue(1);
				}

				break;
			case Opcodes.LCONST_1:
				tlv.setDefaultValue(1);
				break;

			case Opcodes.FCONST_1:
			case Opcodes.DCONST_1:
				tlv.setDefaultValue(1.0);
				break;

			case Opcodes.ICONST_2:
			case Opcodes.FCONST_2:
				tlv.setDefaultValue(2);
				break;

			case Opcodes.ICONST_3:
				tlv.setDefaultValue(3);
				break;

			case Opcodes.ICONST_4:
				tlv.setDefaultValue(4);
				break;

			case Opcodes.ICONST_5:
				tlv.setDefaultValue(5);
				break;

			case Opcodes.BIPUSH:
			case Opcodes.SIPUSH:
				tlv.setDefaultValue(((IntInsnNode) source).operand);
				break;

			case Opcodes.LDC:
				tlv.setDefaultValue(((LdcInsnNode) source).cst);
				break;

			default:
				throw new ParserException("Initialization is not"
						+ " defined for thread local variable " + tlv.getName());
			}
		}
	}
}
