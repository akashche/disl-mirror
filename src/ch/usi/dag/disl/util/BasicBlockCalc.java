package ch.usi.dag.disl.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class BasicBlockCalc {

	// Make sure an instruction has a valid next-instruction.
	// NOTE that in asm, label might be an AbstractInsnNode. If an instruction
	// is followed with a label which is the end of an instruction list, then
	// it has no next instruction.
	public static boolean hasNext(InsnList instr_lst, int i) {

		if (i < instr_lst.size()) {

			AbstractInsnNode nextInstruction = instr_lst.get(i + 1);

			return nextInstruction == null
					|| !(AsmHelper.isVirtualInstr(nextInstruction)
							&& nextInstruction.getNext() == null);
		}

		return false;
	}
	
	// Get basic blocks of the given method node.
	public static List<AbstractInsnNode> getAll(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks, boolean isPrecise) {

		Set<AbstractInsnNode> bb_begins = new HashSet<AbstractInsnNode>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean add(AbstractInsnNode e) {
				return super.add(AsmHelper.skipLabels(e, true));
			}
		};
					
		bb_begins.add(instructions.getFirst());

		for (int i = 0; i < instructions.size(); i++) {
			AbstractInsnNode instruction = instructions.get(i);
			int opcode = instruction.getOpcode();

			switch (instruction.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				bb_begins.add(((JumpInsnNode) instruction).label);

				// goto never returns.
				if (opcode != Opcodes.GOTO && hasNext(instructions, i)) {
					bb_begins.add(instruction.getNext());
				}

				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) instruction;

				for (LabelNode label : lsin.labels) {
					bb_begins.add(label);
				}

				bb_begins.add(lsin.dflt);
				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) instruction;

				for (LabelNode label : tsin.labels) {
					bb_begins.add(label);
				}

				bb_begins.add(tsin.dflt);
				break;
			}

			default:
				break;
			}

			if (isPrecise && AsmHelper.mightThrowException(instruction)) {
				bb_begins.add(instruction.getNext());
			}
		}

		for (TryCatchBlockNode try_catch_block : tryCatchBlocks) {
			bb_begins.add(try_catch_block.handler);
		}

		// Sort
		List<AbstractInsnNode> bb_list = new ArrayList<AbstractInsnNode>();

		for (AbstractInsnNode instruction : instructions.toArray()) {
			if (bb_begins.contains(instruction)) {
				bb_list.add(instruction);
			}
		}

		return bb_list;
	}
}
