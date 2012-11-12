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

	// Get basic blocks of the given method node.
	public static List<AbstractInsnNode> getAll(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks, boolean isPrecise) {

		// add method automatically skips all the labels
		Set<AbstractInsnNode> bbStarts = new HashSet<AbstractInsnNode>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean add(AbstractInsnNode e) {
				return super.add(AsmHelper.skipVirtualInsns(e, true));
			}
		};
					
		bbStarts.add(instructions.getFirst());

		for (int i = 0; i < instructions.size(); i++) {
			
			AbstractInsnNode instruction = instructions.get(i);
			int opcode = instruction.getOpcode();

			switch (instruction.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				bbStarts.add(((JumpInsnNode) instruction).label);

				if (instruction.getOpcode () != Opcodes.GOTO) {
					//
					// There must be a valid (non-virtual) instruction 
					// following a conditional/subroutine jump instruction.
					//
					AbstractInsnNode nextInsn = AsmHelper.nextNonVirtualInsn (instruction);
					if (nextInsn != null) {
						bbStarts.add (nextInsn);
					}
				}
				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) instruction;

				for (LabelNode label : lsin.labels) {
					bbStarts.add(label);
				}

				bbStarts.add(lsin.dflt);
				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) instruction;

				for (LabelNode label : tsin.labels) {
					bbStarts.add(label);
				}

				bbStarts.add(tsin.dflt);
				break;
			}

			default:
				break;
			}

			if (isPrecise && AsmHelper.mightThrowException(instruction)) {
				bbStarts.add(instruction.getNext());
			}
		}

		// add also starts of the handlers
		for (TryCatchBlockNode tryCatchBlock : tryCatchBlocks) {
			bbStarts.add(tryCatchBlock.handler);
		}

		// sort starting instructions
		List<AbstractInsnNode> bbSortedList = new ArrayList<AbstractInsnNode>();

		for (AbstractInsnNode instruction : AsmHelper.allInsnsFrom (instructions)) {
			if (bbStarts.contains(instruction)) {
				bbSortedList.add(instruction);
			}
		}

		return bbSortedList;
	}
}
