package ch.usi.dag.disl.dislclass.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
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
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SnippetUnprocessedCode extends UnprocessedCode {

	private String className;
	private String methodName;
	private Set<String> declaredStaticAnalyses;
	private boolean usesDynamicAnalysis;

	public SnippetUnprocessedCode(String className, String methodName,
			InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticAnalyses, boolean usesDynamicAnalysis) {

		super(instructions, tryCatchBlocks);
		this.className = className;
		this.methodName = methodName;
		this.declaredStaticAnalyses = declaredStaticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
	}

	public SnippetCode process(LocalVars allLVs,
			Map<Class<?>, Proc> processors, boolean useDynamicBypass)
			throws StaticAnalysisException, ReflectionException,
			ProcessorException {

		// process code
		Code code = super.process(allLVs);

		// process snippet code
		
		InsnList instructions = code.getInstructions();
		List<TryCatchBlockNode> tryCatchBlocks = code.getTryCatchBlocks();
		
		// *** CODE PROCESSING ***
		// !NOTE ! : Code processing has to be done before "processors in use"
		// analysis otherwise the instruction reference produced by this
		// analysis may be wrong
		// NOTE: methods are modifying arguments

		if (useDynamicBypass) {
			insertDynamicBypass(instructions, tryCatchBlocks);
		}

		// *** CODE ANALYSIS ***

		Map<String, Method> staticAnalyses = new HashMap<String, Method>();

		Map<Integer, ProcInvocation> invokedProcessors = 
			new HashMap<Integer, ProcInvocation>();

		AbstractInsnNode[] instructionArray = instructions.toArray();
		for (int i = 0; i < instructionArray.length; ++i) {

			AbstractInsnNode instr = instructionArray[i];

			// *** Parse static analysis methods in use ***

			StaticAnalysisMethod anlMtd = insnInvokesStaticAnalysis(
					declaredStaticAnalyses, instr, staticAnalyses.keySet());

			if (anlMtd != null) {
				staticAnalyses.put(anlMtd.getId(), anlMtd.getRefM());
				continue;
			}

			// *** Parse processors in use ***
			// no other modifications to the code should be done before weaving
			// otherwise, produced instruction reference can be invalid

			ProcessorInfo processor = 
				insnInvokesProcessor(instr, i, processors);

			if (processor != null) {
				invokedProcessors.put(processor.getInstrPos(),
						processor.getProcInvoke());
				continue;
			}
		}

		return new SnippetCode(instructions, tryCatchBlocks,
				code.getReferencedSLVs(), code.getReferencedTLVs(),
				code.containsHandledException(), staticAnalyses,
				usesDynamicAnalysis, invokedProcessors);
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

	private class ProcessorInfo {

		private Integer instrPos;
		private ProcInvocation procInvoke;

		public ProcessorInfo(Integer instrPos, ProcInvocation procInvoke) {
			super();
			this.instrPos = instrPos;
			this.procInvoke = procInvoke;
		}

		public Integer getInstrPos() {
			return instrPos;
		}

		public ProcInvocation getProcInvoke() {
			return procInvoke;
		}
	}

	private ProcessorInfo insnInvokesProcessor(AbstractInsnNode instr, int i,
			Map<Class<?>, Proc> processors) throws ProcessorException,
			ReflectionException {

		final String APPLY_METHOD = "apply";

		// check method invocation
		if (!(instr instanceof MethodInsnNode)) {
			return null;
		}

		MethodInsnNode min = (MethodInsnNode) instr;

		// check if the invocation is processor invocation
		if (!(min.owner.equals(Type.getInternalName(Processor.class)) && min.name
				.equals(APPLY_METHOD))) {
			return null;
		}

		// resolve load parameter instruction
		AbstractInsnNode secondParam = instr.getPrevious();
		AbstractInsnNode firstParam = secondParam.getPrevious();

		// first parameter has to be loaded by LDC
		if (firstParam == null || firstParam.getOpcode() != Opcodes.LDC) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - pass the first (class)"
					+ " argument of a ProcMethod.apply method direcltly."
					+ " ex: ProcMethod.apply(ProcMethod.class,"
					+ " ProcessorApplyType.INSIDE_METHOD)");
		}

		// second parameter has to be loaded by GETSTATIC
		if (secondParam == null || secondParam.getOpcode() != Opcodes.GETSTATIC) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - pass the second (type)"
					+ " argument of a ProcMethod.apply method direcltly."
					+ " ex: ProcMethod.apply(ProcMethod.class,"
					+ " ProcessorApplyType.INSIDE_METHOD)");
		}

		Object processorASMType = ((LdcInsnNode) firstParam).cst;

		if (!(processorASMType instanceof Type)) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - unsupported processor type "
					+ processorASMType.getClass().toString());
		}

		ProcessorApplyType procApplyType = ProcessorApplyType
				.valueOf(((FieldInsnNode) secondParam).name);

		Class<?> processorClass = ReflectionHelper
				.resolveClass((Type) processorASMType);

		Proc processor = processors.get(processorClass);

		if (processor == null) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - unknow processor used: "
					+ processorClass.getClass().toString());
		}

		ProcInvocation prcInv = new ProcInvocation(processor, procApplyType);

		// get instruction index

		return new ProcessorInfo(i, prcInv);
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
}
