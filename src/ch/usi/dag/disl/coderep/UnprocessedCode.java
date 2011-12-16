package ch.usi.dag.disl.coderep;

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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class UnprocessedCode {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<String> declaredStaticContexts;
	private boolean usesDynamicContext;
	private boolean usesClassContext;
	
	public UnprocessedCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticContexts, boolean usesDynamicContext,
			boolean usesClassContext) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.declaredStaticContexts = declaredStaticContexts;
		this.usesDynamicContext = usesDynamicContext;
		this.usesClassContext = usesClassContext;
	}

	public Code process(LocalVars allLVs) throws StaticContextGenException,
			ReflectionException {

		// *** CODE ANALYSIS ***

		Set<SyntheticLocalVar> slvList = new HashSet<SyntheticLocalVar>();

		Set<ThreadLocalVar> tlvList = new HashSet<ThreadLocalVar>();

		for (AbstractInsnNode instr : instructions.toArray()) {

			// *** Parse synthetic local variables ***

			SyntheticLocalVar slv = insnUsesField(instr,
					allLVs.getSyntheticLocals());

			if (slv != null) {
				slvList.add(slv);
				continue;
			}

			// *** Parse thread local variables ***

			ThreadLocalVar tlv = insnUsesField(instr, allLVs.getThreadLocals());

			if (tlv != null) {
				tlvList.add(tlv);
				continue;
			}
		}

		// handled exception check
		boolean containsHandledException = 
			containsHandledException(instructions, tryCatchBlocks);
		
		// *** CODE PROCESSING ***
		// NOTE: methods are modifying arguments

		// replace returns with goto in snippet (in asm code)
		AsmHelper.replaceRetWithGoto(instructions);
		
		// translate thread local variables
		translateThreadLocalVars(instructions, tlvList);

		// *** CODE ANALYSIS ***
		
		Map<String, StaticContextMethod> staticContexts = 
				new HashMap<String, StaticContextMethod>();
		
		AbstractInsnNode[] instructionArray = instructions.toArray();
		for (int i = 0; i < instructionArray.length; ++i) {

			AbstractInsnNode instr = instructionArray[i];

			// *** Parse static context methods in use ***

			StaticContextMethod scm = insnInvokesStaticContext(
					declaredStaticContexts, instr, staticContexts.keySet());

			if (scm != null) {
				staticContexts.put(scm.getId(), scm);
				continue;
			}
		}

		return new Code(instructions, tryCatchBlocks, slvList, tlvList,
				new HashSet<StaticContextMethod>(staticContexts.values()),
				usesDynamicContext, usesClassContext, containsHandledException);
	}
	
	/**
	 * Determines if the instruction invokes some StaticContext class
	 * 
	 * @param knownStAnClasses known StaticContext classes
	 * @param instr instruction to analyze
	 * @param knownMethods methods that are already known
	 * @return 
	 * @throws StaticContextGenException
	 * @throws ReflectionException
	 */
	private StaticContextMethod insnInvokesStaticContext(
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
				|| methodReturn.equals(Type.SHORT_TYPE)
				|| methodReturn.equals(Type.getType(String.class))
				)) {

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

		return new StaticContextMethod(methodID, stAnMethod, stAnClass);
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
	
	/**
	 * Determines if the code contains handler that handles exception and
	 * doesn't propagate some exception further.
	 * 
	 * This has to be detected because it can cause stack inconsistency that has
	 * to be handled in the weaver.
	 */
	private boolean containsHandledException(InsnList instructions,
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

	private void translateThreadLocalVars(InsnList instructions,
			Set<ThreadLocalVar> threadLocalVars) {

		// generate set of ids - better lookup
		Set<String> tlvIDs = new HashSet<String>();
		for(ThreadLocalVar tlv : threadLocalVars) {
			tlvIDs.add(tlv.getID());
		}
		
		for (AbstractInsnNode instr : instructions.toArray()) {

			int opcode = instr.getOpcode();

			// test if it is static field instruction
			if (!(opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC)) {
				continue;
			}
			
			FieldInsnNode fieldInstr = (FieldInsnNode) instr;
			String fieldId = fieldInstr.owner + ThreadLocalVar.NAME_DELIM
					+ fieldInstr.name;
			
			// test if it is thread local variable
			if (! tlvIDs.contains(fieldId)) {
				continue;
			}
			
			final String threadInternalName = 
				Type.getType(Thread.class).getInternalName();
			
			final String currentThreadMethod = "currentThread";
			
			final String currentThreadMethodDesc = 
				"()L" + threadInternalName + ";";

			// insert currentThread method invocation
			instructions.insertBefore(instr, new MethodInsnNode(
					Opcodes.INVOKESTATIC, threadInternalName,
					currentThreadMethod, currentThreadMethodDesc));

			if (opcode == Opcodes.GETSTATIC) {

				// insert new field access
				instructions.insertBefore(instr, new FieldInsnNode(
						Opcodes.GETFIELD, threadInternalName, fieldInstr.name,
						fieldInstr.desc));
			} 
			
			if (opcode == Opcodes.PUTSTATIC) {

				// we need to invoke putfield which has an opposite order of
				// arguments on a stack, than we have now
				//  - so we need to rearrange the arguments
				// - there is no easier general solution unless, we want to
				// track, where the value was pushed on a stack and put
				// currentThread invocation before it
				if (Type.getType(fieldInstr.desc).getSize() == 1) {
					
					// rearrange for single size operand
					instructions
							.insertBefore(instr, new InsnNode(Opcodes.SWAP));
				} else {
					// rearrange for double size operand
					instructions.insertBefore(instr, new InsnNode(
							Opcodes.DUP_X2));
					instructions.insertBefore(instr, new InsnNode(Opcodes.POP));
				}

				// insert new field access
				instructions.insertBefore(instr, new FieldInsnNode(
						Opcodes.PUTFIELD, threadInternalName, fieldInstr.name,
						fieldInstr.desc));
			}

			// remove translated instruction
			instructions.remove(instr);
		}
	}
}
