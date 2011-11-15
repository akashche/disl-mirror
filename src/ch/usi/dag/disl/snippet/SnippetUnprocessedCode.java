package ch.usi.dag.disl.snippet;

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

import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.coderep.UnprocessedCode;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.processor.ProcessorMode;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.generator.struct.Proc;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SnippetUnprocessedCode extends UnprocessedCode {

	private String className;
	private String methodName;
	private Set<String> declaredStaticContexts;
	private boolean usesDynamicContext;
	private boolean dynamicBypass;

	public SnippetUnprocessedCode(String className, String methodName,
			InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticContexts, boolean usesDynamicContext,
			boolean dynamicBypass) {
		super(instructions, tryCatchBlocks);
		this.className = className;
		this.methodName = methodName;
		this.declaredStaticContexts = declaredStaticContexts;
		this.usesDynamicContext = usesDynamicContext;
		this.dynamicBypass = dynamicBypass;
	}

	public SnippetCode process(LocalVars allLVs,
			Map<Type, Proc> processors, Marker marker, boolean allDynamicBypass)
			throws StaticContextGenException, ReflectionException,
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

		if (allDynamicBypass || dynamicBypass) {
			insertDynamicBypass(instructions, tryCatchBlocks);
		}

		// *** CODE ANALYSIS ***

		Map<String, StaticContextMethod> staticContexts = 
			new HashMap<String, StaticContextMethod>();

		Map<Integer, ProcInvocation> invokedProcessors = 
			new HashMap<Integer, ProcInvocation>();

		AbstractInsnNode[] instructionArray = instructions.toArray();
		for (int i = 0; i < instructionArray.length; ++i) {

			AbstractInsnNode instr = instructionArray[i];

			// *** Parse static context methods in use ***

			StaticContextData anlMtd = insnInvokesStaticContext(
					declaredStaticContexts, instr, staticContexts.keySet());

			if (anlMtd != null) {
				staticContexts.put(anlMtd.getId(), anlMtd.getRefM());
				continue;
			}

			// *** Parse processors in use ***
			// no other modifications to the code should be done before weaving
			// otherwise, produced instruction reference can be invalid

			ProcessorInfo processor = 
				insnInvokesProcessor(instr, i, processors, marker);

			if (processor != null) {
				invokedProcessors.put(processor.getInstrPos(),
						processor.getProcInvoke());
				continue;
			}
		}

		return new SnippetCode(instructions, tryCatchBlocks,
				code.getReferencedSLVs(), code.getReferencedTLVs(),
				code.containsHandledException(), staticContexts,
				usesDynamicContext, invokedProcessors);
	}

	class StaticContextData {

		private String id;
		private StaticContextMethod refM;

		public StaticContextData(String id, StaticContextMethod refM) {
			super();
			this.id = id;
			this.refM = refM;
		}

		public String getId() {
			return id;
		}

		public StaticContextMethod getRefM() {
			return refM;
		}
	}

	private StaticContextData insnInvokesStaticContext(
			Set<String> knownStAnClasses, AbstractInsnNode instr,
			Set<String> knownMethods) throws StaticContextGenException,
			ReflectionException {

		// check - instruction invokes method
		if (!(instr instanceof MethodInsnNode)) {
			return null;
		}

		MethodInsnNode methodInstr = (MethodInsnNode) instr;

		// check - we've found static context
		if (!knownStAnClasses.contains(methodInstr.owner)) {
			return null;
		}

		// crate ASM Method object
		org.objectweb.asm.commons.Method asmMethod = 
			new org.objectweb.asm.commons.Method(methodInstr.name,
					methodInstr.desc);

		// check method argument
		// no argument is allowed
		Type[] methodArguments = asmMethod.getArgumentTypes();

		if (methodArguments.length != 0) {
			throw new StaticContextGenException("Static context method "
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

			throw new StaticContextGenException("Static context method "
					+ methodInstr.name + " in the class " + methodInstr.owner
					+ " can have only basic type or String as a return type.");
		}

		// crate static context method id
		String methodID = methodInstr.owner
				+ Constants.STATIC_CONTEXT_METHOD_DELIM + methodInstr.name;

		if (knownMethods.contains(methodID)) {
			return null;
		}

		// resolve static context class
		Class<?> stAnClass = ReflectionHelper.resolveClass(Type
				.getObjectType(methodInstr.owner));

		Method stAnMethod = ReflectionHelper.resolveMethod(stAnClass,
				methodInstr.name);

		StaticContextMethod stAnM =
			new StaticContextMethod(stAnMethod, stAnClass);
		
		return new StaticContextData(methodID, stAnM);
	}

	private static class ProcessorInfo {

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
			Map<Type, Proc> processors, Marker marker)
			throws ProcessorException, ReflectionException {

		final String APPLY_METHOD = "apply";

		// check method invocation
		if (!(instr instanceof MethodInsnNode)) {
			return null;
		}

		MethodInsnNode min = (MethodInsnNode) instr;

		// check if the invocation is processor invocation
		if (!(min.owner.equals(Type.getInternalName(Processor.class))
				&& min.name.equals(APPLY_METHOD))) {
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
					+ " ProcessorMode.METHOD_ARGS)");
		}

		// second parameter has to be loaded by GETSTATIC
		if (secondParam == null || secondParam.getOpcode() != Opcodes.GETSTATIC) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - pass the second (type)"
					+ " argument of a ProcMethod.apply method direcltly."
					+ " ex: ProcMethod.apply(ProcMethod.class,"
					+ " ProcessorMode.METHOD_ARGS)");
		}

		Object asmType = ((LdcInsnNode) firstParam).cst;

		if (!(asmType instanceof Type)) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - unsupported processor type "
					+ asmType.getClass().toString());
		}

		Type processorType = (Type) asmType;
		
		ProcessorMode procApplyType = ProcessorMode
				.valueOf(((FieldInsnNode) secondParam).name);
		
		// if the processor apply type is CALLSITE_ARGS
		// the only allowed marker is BytecodeMarker
		if(ProcessorMode.CALLSITE_ARGS.equals(procApplyType)
				&& marker.getClass() != BytecodeMarker.class) {
			throw new ProcessorException(
					"ArgsProcessor applied in mode CALLSITE_ARGS in method "
					+ className + "." + methodName
					+ " can be used only with BytecodeMarker");
		}

		Proc processor = processors.get(processorType);

		if (processor == null) {
			throw new ProcessorException("In advice " + className + "."
					+ methodName + " - unknow processor used: "
					+ processorType.getClassName());
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

		// TODO ! snippet should not throw an exception - the solution should contain try-finally for each block regardless of dynamic bypass and should fail immediately
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
