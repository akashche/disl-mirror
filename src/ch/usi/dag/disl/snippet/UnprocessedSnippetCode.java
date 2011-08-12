package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.snippet.localvars.LocalVars;
import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class UnprocessedSnippetCode {

	String className;
	String methodName;
	InsnList instructions;
	List<TryCatchBlockNode> tryCatchBlocks;
	Set<String> declaredStaticAnalyses;
	boolean usesDynamicAnalysis;

	public UnprocessedSnippetCode(String className, String methodName,
			InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticAnalyses, boolean usesDynamicAnalysis) {
		super();
		this.className = className;
		this.methodName = methodName;
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.declaredStaticAnalyses = declaredStaticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
	}

	public SnippetCode process(LocalVars allLVs, boolean useDynamicBypass)
			throws StaticAnalysisException, ReflectionException {

		// *** CODE ANALYSIS ***

		Set<SyntheticLocalVar> slvList = new HashSet<SyntheticLocalVar>();

		Set<ThreadLocalVar> tlvList = new HashSet<ThreadLocalVar>();

		Map<String, Method> staticAnalyses = new HashMap<String, Method>();

		for (AbstractInsnNode instr : instructions.toArray()) {

			// *** Parse synthetic local variables ***

			SyntheticLocalVar slv = insnUsesField(instr,
					allLVs.getSyntheticLocals());

			if (slv != null) {
				slvList.add(slv);
				continue;
			}

			// *** Parse synthetic local variables ***

			ThreadLocalVar tlv = insnUsesField(instr, allLVs.getThreadLocals());

			if (tlv != null) {
				tlvList.add(tlv);
				continue;
			}

			// *** Parse static analysis methods in use ***

			StaticAnalysisMethod anlMtd = insnInvokesStaticAnalysis(
					declaredStaticAnalyses, instr, staticAnalyses.keySet());

			if (anlMtd != null) {
				staticAnalyses.put(anlMtd.getId(), anlMtd.getRefM());
				continue;
			}
		}

		// handled exception check
		boolean containsHandledException = 
			containsHandledException(instructions, tryCatchBlocks);
		
		// TODO ! analysis checking
		// arguments (local variables 1, 2, ...) may be used only in method
		// calls

		// TODO ! dynamic analysis method argument checking
		// values of arguments should be only constant values

		// *** CODE PROCESSING ***
		// NOTE: methods are modifying arguments

		// remove returns in snippet (in asm code)
		AsmHelper.removeReturns(instructions);

		if (!useDynamicBypass) {
			insertDynamicBypass(instructions, tryCatchBlocks);
		}

		return new SnippetCode(instructions, tryCatchBlocks, slvList, tlvList,
				staticAnalyses, usesDynamicAnalysis, containsHandledException);
	}

	/**
	 * Determines if the instruction uses some field defined in
	 * allPossibleFieldNames map. If the field is found in supplied map, the
	 * corresponding mapped object is returned.
	 * 
	 * @param <T>
	 *            type of the return value
	 * @param instr
	 *            instruction to test
	 * @param allPossibleFieldNames
	 *            map with all possible field names as keys
	 * @return object from a map, that corresponds with matched field name
	 */
	private <T> T insnUsesField(AbstractInsnNode instr,
			Map<String, T> allPossibleFieldNames) {

		// check - instruction uses field
		if (!(instr instanceof FieldInsnNode)) {
			return null;
		}

		FieldInsnNode fieldInstr = (FieldInsnNode) instr;

		// get whole name of the field
		String wholeFieldName = fieldInstr.owner + SyntheticLocalVar.NAME_DELIM
				+ fieldInstr.name;

		// check - it is SyntheticLocal variable (it's defined in snippet)
		return allPossibleFieldNames.get(wholeFieldName);
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

	/**
	 * Determines if the code contains handler that handles exception and
	 * doesn't propagate some exception further.
	 * 
	 * This has to be detected because it can cause stack inconsistency that has
	 * to be handled in the weaver.
	 */
	public static boolean containsHandledException(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {

		if (tryCatchBlocks.size() == 0) {
			return false;
		}

		// create control flow graph
		CtrlFlowGraph cfg = new CtrlFlowGraph(instructions, tryCatchBlocks);
		cfg.visit(instructions.getFirst());

		// check if the control flow continues after exception handler
		// if it does, exception was handled
		for (int i = tryCatchBlocks.size() - 1; i >= 0; --i) {

			TryCatchBlockNode tcb = tryCatchBlocks.get(i);

			if (cfg.visit(tcb.handler).size() != 0) {
				return true;
			}
		}

		return false;
	}

	public static boolean useConstAnalysis(MethodNode method) {
		
		Type[] types = Type.getArgumentTypes(method.desc);
		int index = 0;		
		int local = 0;

		for (int i = 0; i < types.length; i++) {

			if (types[i].equals(Type.DOUBLE_TYPE)
					|| types[i].equals(Type.LONG_TYPE)) {
				index += 2;
			} else {
				index += 1;
			}
		}
		
		// The following code assumes that all disl advices is static 
		for (AbstractInsnNode instr : method.instructions.toArray()) {

			switch (instr.getOpcode()) {
			case Opcodes.ALOAD:
				
				local = ((VarInsnNode) instr).var;

				if (local >= 0 && local < index
						&& instr.getNext().getOpcode() == Opcodes.ASTORE) {
					return false;
				}

				break;
				
			case Opcodes.ASTORE:

				local = ((VarInsnNode) instr).var;

				if (local >= 0 && local < index) {
					return false;
				}

				break;
				
			default:
				continue;
			}
		}
		
		return true;
	}

	public static boolean passConstToDynamicAnalysis(InsnList instructions) {
	
		for (AbstractInsnNode instr : instructions.toArray()) {

			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}
			
			AbstractInsnNode prev = instr.getPrevious();
			
			if (AsmHelper.getType(prev) == null) {
				return false;
			}
			
			switch (prev.getPrevious().getOpcode()) {
			case Opcodes.ICONST_M1:
			case Opcodes.ICONST_0:
			case Opcodes.ICONST_1:
			case Opcodes.ICONST_2:
			case Opcodes.ICONST_3:
			case Opcodes.ICONST_4:
			case Opcodes.ICONST_5:
			case Opcodes.BIPUSH:
				break;

			default:
				return false;
			}
		}
		
		return true;
	}

}
