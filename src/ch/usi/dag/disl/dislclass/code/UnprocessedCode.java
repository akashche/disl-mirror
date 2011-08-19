package ch.usi.dag.disl.dislclass.code;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class UnprocessedCode {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	
	public UnprocessedCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
	}

	public Code process(LocalVars allLVs) throws StaticAnalysisException,
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

		// remove returns in snippet (in asm code)
		AsmHelper.removeReturns(instructions);
		
		// translate thread local variables
		translateThreadLocalVars(instructions, tlvList);

		return new Code(instructions, tryCatchBlocks, slvList, tlvList,
				containsHandledException);
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

			// Only field instructions will be transformed
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {

				FieldInsnNode fieldInstr = (FieldInsnNode) instr;
				String fieldId = fieldInstr.owner + ThreadLocalVar.NAME_DELIM
						+ fieldInstr.name;

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

				// insert new field access (get or put)
				int fieldOpcode = (opcode == Opcodes.GETSTATIC) ? 
						Opcodes.GETFIELD : Opcodes.PUTFIELD;
				
				instructions.insertBefore(instr, new FieldInsnNode(
						fieldOpcode, threadInternalName, fieldInstr.name,
						fieldInstr.desc));
				
				// remove translated instruction
				instructions.remove(instr);
			}
		}
	}
}