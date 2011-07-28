package ch.usi.dag.disl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class InsnListHelper {

	// Create a label node.
	// TODO remove it when LabelNode is fixed in ASM
	public static LabelNode createLabel() {
		Label label = new Label();
		LabelNode labelNode = new LabelNode(label);
		label.info = labelNode;
		return labelNode;
	}

	public static boolean isReturn(int opcode) {
		return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
	}

	public static AbstractInsnNode skipLabels(AbstractInsnNode instr,
			boolean isForward) {
		while (instr != null && instr.getOpcode() == -1) {
			instr = isForward ? instr.getNext() : instr.getPrevious();
		}
		
		return instr;
	}

	public static void removeReturns(InsnList ilst) {
		// Remove 'return' instruction
		List<AbstractInsnNode> returns = new LinkedList<AbstractInsnNode>();

		for (AbstractInsnNode instr : ilst.toArray()) {
			int opcode = instr.getOpcode();

			if (isReturn(opcode)) {
				returns.add(instr);
			}
		}

		if (returns.size() > 1) {
			// Replace 'return' instructions with 'goto'
			LabelNode label = new LabelNode(new Label());
			ilst.add(label);

			for (AbstractInsnNode instr : returns) {
				ilst.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO, label));
				ilst.remove(instr);
			}
		} else if (returns.size() == 1) {
			ilst.remove(returns.get(0));
		}
	}

	// TODO ! remove
	// Make a clone of an instruction list
	public static InsnList cloneList(InsnList src) {

		Map<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>();

		InsnList dst = new InsnList();

		// First iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : src.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = new LabelNode(new Label());
				map.put((LabelNode) instr, label);
			}
		}

		// then copy instructions using clone
		AbstractInsnNode instr = src.getFirst();
		while (instr != src.getLast().getNext()) {

			// special case where we put a new label instead of old one
			if (instr instanceof LabelNode) {

				dst.add(map.get(instr));

				instr = instr.getNext();

				continue;
			}

			dst.add(instr.clone(map));

			instr = instr.getNext();
		}

		return dst;
	}

	// Detects if the instruction list contains only return
	public static boolean containsOnlyReturn(InsnList ilst) {

		return isReturn(ilst.getFirst().getOpcode());
	}

	// Make sure an instruction has a valid next-instruction.
	// NOTE that in asm, label might be an AbstractInsnNode. If an instruction
	// is followed with a label which is the end of an instruction list, then
	// it has no next instruction.
	public static boolean hasNext(InsnList instr_lst, int i) {
		
		if (i < instr_lst.size()) {
			
			AbstractInsnNode nextInstruction = instr_lst.get(i + 1);
			
			return nextInstruction == null
					|| !(nextInstruction.getOpcode() == -1
					&& nextInstruction.getNext() == null);
		}

		return false;
	}

	public static boolean isBranch(AbstractInsnNode instruction) {
		
		int opcode = instruction.getOpcode();

		return instruction instanceof JumpInsnNode
				|| instruction instanceof LookupSwitchInsnNode
				|| instruction instanceof TableSwitchInsnNode
				|| opcode == Opcodes.ATHROW || opcode == Opcodes.RET
				|| (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
	}

	public static boolean isConditionalBranch(AbstractInsnNode instruction) {
		
		int opcode = instruction.getOpcode();

		return (instruction instanceof JumpInsnNode && opcode != Opcodes.GOTO);
	}

	// Get basic blocks of the given method node.
	public static List<AbstractInsnNode> getBasicBlocks(MethodNode method,
			boolean isPrecise) {
		
		InsnList instr_lst = method.instructions;

		Set<AbstractInsnNode> bb_begins = new HashSet<AbstractInsnNode>();
		bb_begins.add(instr_lst.getFirst());

		for (int i = 0; i < instr_lst.size(); i++) {
			AbstractInsnNode instruction = instr_lst.get(i);
			int opcode = instruction.getOpcode();

			switch (instruction.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				bb_begins.add(((JumpInsnNode) instruction).label);

				// goto never returns.
				if (opcode != Opcodes.GOTO && hasNext(instr_lst, i)) {
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

			if (isPrecise && mightThrowException(instruction)) {
				bb_begins.add(instruction.getNext());
			}
		}

		for (TryCatchBlockNode try_catch_block : method.tryCatchBlocks) {
			bb_begins.add(try_catch_block.handler);
		}

		// Sort
		List<AbstractInsnNode> bb_list = new ArrayList<AbstractInsnNode>();

		for (AbstractInsnNode instruction : instr_lst.toArray()) {
			if (bb_begins.contains(instruction)) {
				bb_list.add(instruction);
			}
		}

		return bb_list;
	}

	public static boolean mightThrowException(AbstractInsnNode instruction) {

		switch (instruction.getOpcode()) {

			// NullPointerException, ArrayIndexOutOfBoundsException
		case Opcodes.BALOAD:
		case Opcodes.DALOAD:
		case Opcodes.FALOAD:
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.DASTORE:
		case Opcodes.FASTORE:
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.AALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
		case Opcodes.SASTORE:
			// NullPointerException, ArrayIndexOutOfBoundsException,
			// ArrayStoreException
		case Opcodes.AASTORE:
			// NullPointerException
		case Opcodes.ARRAYLENGTH:
		case Opcodes.ATHROW:
		case Opcodes.GETFIELD:
		case Opcodes.PUTFIELD:
			// NullPointerException, StackOverflowError
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKEVIRTUAL:
			// StackOverflowError
		case Opcodes.INVOKESTATIC:
			// NegativeArraySizeException
		case Opcodes.ANEWARRAY:
			// NegativeArraySizeException, OutOfMemoryError
		case Opcodes.NEWARRAY:
		case Opcodes.MULTIANEWARRAY:
			// OutOfMemoryError, InstantiationError
		case Opcodes.NEW:
			// OutOfMemoryError
		case Opcodes.LDC:
			// ClassCastException
		case Opcodes.CHECKCAST:
			// ArithmeticException
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.LDIV:
		case Opcodes.LREM:
			// New instruction in JDK7
		case Opcodes.INVOKEDYNAMIC:
			return true;
		default:
			return false;
		}
	}
}
