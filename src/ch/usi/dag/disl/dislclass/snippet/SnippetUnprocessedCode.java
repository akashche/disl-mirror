package ch.usi.dag.disl.dislclass.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.processor.Processors;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SnippetUnprocessedCode extends UnprocessedCode {

	protected Set<String> declaredStaticAnalyses;
	protected boolean usesDynamicAnalysis;

	public SnippetUnprocessedCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticAnalyses, boolean usesDynamicAnalysis) {
		
		super(instructions, tryCatchBlocks);
		this.declaredStaticAnalyses = declaredStaticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
	}


	public SnippetCode process(LocalVars allLVs, boolean useDynamicBypass)
			throws StaticAnalysisException, ReflectionException {
		
		// process code
		Code code = super.process(allLVs);
		
		// process snippet code
		
		// *** CODE ANALYSIS ***
		
		InsnList instructions = code.getInstructions();
		List<TryCatchBlockNode> tryCatchBlocks = code.getTryCatchBlocks();
		
		Map<String, Method> staticAnalyses = new HashMap<String, Method>();
		
		for (AbstractInsnNode instr : instructions.toArray()) {
			
			// *** Parse static analysis methods in use ***

			StaticAnalysisMethod anlMtd = insnInvokesStaticAnalysis(
					declaredStaticAnalyses, instr, staticAnalyses.keySet());

			if (anlMtd != null) {
				staticAnalyses.put(anlMtd.getId(), anlMtd.getRefM());
				continue;
			}
		}
		
		// *** CODE PROCESSING ***
		// NOTE: methods are modifying arguments
		
		if (useDynamicBypass) {
			insertDynamicBypass(instructions, tryCatchBlocks);
		}
		
		return new SnippetCode(instructions, tryCatchBlocks,
				code.getReferencedSLV(), code.getReferencedTLV(),
				code.containsHandledException(), staticAnalyses,
				usesDynamicAnalysis);
	}

	class StaticAnalysisMethod {

		private String id;
		private Method refM;

		public StaticAnalysisMethod(String id, Method refM) {
			super();
			this.id = id;
			this.refM = refM;
		}

		public String getId() {
			return id;
		}

		public Method getRefM() {
			return refM;
		}
	}

	private StaticAnalysisMethod insnInvokesStaticAnalysis(
			Set<String> knownStAnClasses, AbstractInsnNode instr,
			Set<String> knownMethods) throws StaticAnalysisException,
			ReflectionException {

		// check - instruction invokes method
		if (!(instr instanceof MethodInsnNode)) {
			return null;
		}

		MethodInsnNode methodInstr = (MethodInsnNode) instr;

		// check - we've found static analysis
		if (!knownStAnClasses.contains(methodInstr.owner)) {
			return null;
		}

		// crate ASM Method object
		org.objectweb.asm.commons.Method asmMethod = new org.objectweb.asm.commons.Method(
				methodInstr.name, methodInstr.desc);

		// check method argument
		// no argument is allowed
		Type[] methodArguments = asmMethod.getArgumentTypes();

		if (methodArguments.length != 0) {
			throw new StaticAnalysisException("Static analysis method "
					+ methodInstr.name + " in the class " + methodInstr.owner
					+ " shouldn't have a parameter.");
		}

		Type methodReturn = asmMethod.getReturnType();

		// only basic types + String are allowed as return type
		if (!(methodReturn.equals(Type.BOOLEAN_TYPE)
				|| methodReturn.equals(Type.BYTE_TYPE)
				|| methodReturn.equals(Type.CHAR_TYPE)
				|| methodReturn.equals(Type.DOUBLE_TYPE)
				|| methodReturn.equals(Type.FLOAT_TYPE)
				|| methodReturn.equals(Type.INT_TYPE)
				|| methodReturn.equals(Type.LONG_TYPE)
				|| methodReturn.equals(Type.SHORT_TYPE) || methodReturn
				.equals(Type.getType(String.class)))) {

			throw new StaticAnalysisException("Static analysis method "
					+ methodInstr.name + " in the class " + methodInstr.owner
					+ " can have only basic type or String as a return type.");
		}

		// crate static analysis method id
		String methodID = methodInstr.owner
				+ Constants.STATIC_ANALYSIS_METHOD_DELIM + methodInstr.name;

		if (knownMethods.contains(methodID)) {
			return null;
		}

		// resolve static analysis class
		Class<?> stAnClass = ReflectionHelper.resolveClass(Type
				.getObjectType(methodInstr.owner));

		Method method = ReflectionHelper.resolveMethod(stAnClass,
				methodInstr.name);

		return new StaticAnalysisMethod(methodID, method);
	}

	private void insertDynamicBypass(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {

		// inserts
		// DynamicBypass.activate();
		// try {
		// ... original code
		// } finally {
		// DynamicBypass.deactivate();
		// }

		// create method nodes
		Type typeDB = Type.getType(DynamicBypass.class);
		MethodInsnNode mtdActivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "activate", "()V");
		MethodInsnNode mtdDeactivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "deactivate", "()V");

		// add try label at the beginning
		LabelNode tryBegin = new LabelNode();
		instructions.insert(tryBegin);

		// add invocation of activate at the beginning
		instructions.insert(mtdActivate.clone(null));

		// ## try {

		// ## }

		// add try label at the end
		LabelNode tryEnd = new LabelNode();
		instructions.add(tryEnd);

		// ## after normal flow

		// add invocation of deactivate - normal flow
		instructions.add(mtdDeactivate.clone(null));

		// normal flow should jump after handler
		LabelNode handlerEnd = new LabelNode();
		instructions.add(new JumpInsnNode(Opcodes.GOTO, handlerEnd));

		// ## after abnormal flow - exception handler

		// add handler begin
		LabelNode handlerBegin = new LabelNode();
		instructions.add(handlerBegin);

		// add invocation of deactivate - abnormal flow
		instructions.add(mtdDeactivate.clone(null));
		// throw exception again
		instructions.add(new InsnNode(Opcodes.ATHROW));

		// add handler end
		instructions.add(handlerEnd);

		// ## add handler to the list
		tryCatchBlocks.add(new TryCatchBlockNode(tryBegin, tryEnd,
				handlerBegin, null));
	}
	
	public static class ProcessorInfo {
		public AbstractInsnNode loc;
		public Type clazz;
		public String type;
	}

	public static List<ProcessorInfo> detect(MethodNode method) {

		List<ProcessorInfo> processors = new LinkedList<ProcessorInfo>();

		for (AbstractInsnNode instr : method.instructions.toArray()) {

			if (!(instr instanceof MethodInsnNode)) {
				continue;
			}

			MethodInsnNode min = (MethodInsnNode) instr;

			if (!(min.owner.equals(Type.getInternalName(Processors.class)) && min.name
					.equals("apply"))) {
				continue;
			}

			ProcessorInfo processor = new ProcessorInfo();
			AbstractInsnNode prev = instr.getPrevious();

			if (prev == null || prev.getOpcode() != Opcodes.GETSTATIC) {
				continue;
			}

			processor.type = ((FieldInsnNode) prev).name;

			AbstractInsnNode pprev = prev.getPrevious();

			if (pprev == null || pprev.getOpcode() != Opcodes.LDC) {
				continue;
			}

			Object clazz = ((LdcInsnNode) pprev).cst;

			if (!(clazz instanceof Type)) {
				continue;
			}

			processor.clazz = (Type) clazz;
			processor.loc = pprev;

			processors.add(processor);
		}

		return processors;
	}
	
}
