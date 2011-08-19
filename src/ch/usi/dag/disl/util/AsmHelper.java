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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.exception.DiSLFatalException;

public class AsmHelper {

	public static int getIConstOperand(AbstractInsnNode instr) {

		switch (instr.getOpcode()) {
		case Opcodes.ICONST_M1:
			return -1;
		case Opcodes.ICONST_0:
			return 0;
		case Opcodes.ICONST_1:
			return 1;
		case Opcodes.ICONST_2:
			return 2;
		case Opcodes.ICONST_3:
			return 3;
		case Opcodes.ICONST_4:
			return 4;
		case Opcodes.ICONST_5:
			return 5;
		case Opcodes.BIPUSH:
			return ((IntInsnNode) instr).operand;
		default:
			throw new DiSLFatalException("Parameter index out of bound");
		}
	}
	
	public static AbstractInsnNode getIConstInstr(int iconst) {

		switch (iconst) {
		case -1:
			return new InsnNode(Opcodes.ICONST_M1);
		case 0:
			return new InsnNode(Opcodes.ICONST_0);
		case 1:
			return new InsnNode(Opcodes.ICONST_1);
		case 2:
			return new InsnNode(Opcodes.ICONST_2);
		case 3:
			return new InsnNode(Opcodes.ICONST_3);
		case 4:
			return new InsnNode(Opcodes.ICONST_4);
		case 5:
			return new InsnNode(Opcodes.ICONST_5);
		default:
			return new IntInsnNode(Opcodes.BIPUSH, iconst);
		}
	}

	public static int numberOfOccupiedSlots(Type type) {
	
		if (type.equals(Type.DOUBLE_TYPE) || type.equals(Type.LONG_TYPE)) {
			return 2;
		}
		
		return 1;
	}
	
	public static int getParameterIndex(MethodNode method, int par_index) {

		Type[] types = Type.getArgumentTypes(method.desc);

		if (par_index >= types.length) {
			throw new DiSLFatalException("Parameter index out of bound");
		}

		int index = 0;

		for (int i = 0; i < par_index; i++) {

			index += numberOfOccupiedSlots(types[i]);
		}

		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			index += 1;
		}

		return index;
	}

	/**
	 * Returns type if the int.class or String.class construct is used
	 */
	public static Type getClassType(AbstractInsnNode instr) {

		switch (instr.getOpcode()) {
		// type for basic class types int, float,...
		case Opcodes.GETSTATIC:

			String owner = ((FieldInsnNode) instr).owner;

			if (owner.endsWith("Boolean")) {
				return Type.BOOLEAN_TYPE;
			} else if (owner.endsWith("Byte")) {
				return Type.BYTE_TYPE;
			} else if (owner.endsWith("Character")) {
				return Type.CHAR_TYPE;
			} else if (owner.endsWith("Double")) {
				return Type.DOUBLE_TYPE;
			} else if (owner.endsWith("Float")) {
				return Type.FLOAT_TYPE;
			} else if (owner.endsWith("Integer")) {
				return Type.INT_TYPE;
			} else if (owner.endsWith("Long")) {
				return Type.LONG_TYPE;
			} else if (owner.endsWith("Short")) {
				return Type.SHORT_TYPE;
			}

			return null;

		// type for object types String,...
		case Opcodes.LDC:

			Object tObj = ((LdcInsnNode) instr).cst;

			if (tObj instanceof Type) {
				return (Type) tObj;
			}
			
			return null;

		default:
			return null;
		}

	}

	public static AbstractInsnNode remove(InsnList ilst,
			AbstractInsnNode instr, boolean forward) {
		AbstractInsnNode ret = forward ? instr.getNext() : instr.getPrevious();

		ilst.remove(instr);
		return ret;
	}

	public static AbstractInsnNode removeIf(InsnList ilst,
			AbstractInsnNode instr, int opcode, boolean forward) {

		if (instr.getOpcode() == opcode) {
			return remove(ilst, instr, forward);
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

	// Make a clone of an instruction list
	public static InsnList cloneInsnList(InsnList src) {

		Map<LabelNode, LabelNode> map = createLabelMap(src);
		return cloneInsnList(src, map);
	}

	public static Map<LabelNode, LabelNode> createLabelMap(InsnList src) {

		Map<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>();

		// Iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : src.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = new LabelNode(new Label());
				map.put((LabelNode) instr, label);
			}
		}

		return map;
	}

	public static InsnList cloneInsnList(InsnList src,
			Map<LabelNode, LabelNode> map) {

		InsnList dst = new InsnList();

		// Copy instructions using clone
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

	public static List<TryCatchBlockNode> cloneTryCatchBlocks(
			List<TryCatchBlockNode> src, Map<LabelNode, LabelNode> map) {

		List<TryCatchBlockNode> dst = new LinkedList<TryCatchBlockNode>();

		for (TryCatchBlockNode tcb : src) {

			dst.add(new TryCatchBlockNode(map.get(tcb.start), map.get(tcb.end),
					map.get(tcb.handler), tcb.type));
		}

		return dst;
	}

	public static AbstractInsnNode skipLabels(AbstractInsnNode instr,
			boolean isForward) {
		while (instr != null && isVirtualInstr(instr)) {
			instr = isForward ? instr.getNext() : instr.getPrevious();
		}

		return instr;
	}

	public static boolean isVirtualInstr(AbstractInsnNode instr) {
		
		return instr.getOpcode() == -1;
	}
	
	// Detects if the instruction list contains only return
	public static boolean containsOnlyReturn(InsnList ilst) {

		AbstractInsnNode instr = ilst.getFirst();
		
		while(instr != null && isVirtualInstr(instr)) {
			instr = instr.getNext();
		}
		
		return isReturn(instr.getOpcode());
	}

	// Make sure an instruction has a valid next-instruction.
	// NOTE that in asm, label might be an AbstractInsnNode. If an instruction
	// is followed with a label which is the end of an instruction list, then
	// it has no next instruction.
	public static boolean hasNext(InsnList instr_lst, int i) {

		if (i < instr_lst.size()) {

			AbstractInsnNode nextInstruction = instr_lst.get(i + 1);

			return nextInstruction == null
					|| !(isVirtualInstr(nextInstruction)
							&& nextInstruction.getNext() == null);
		}

		return false;
	}

	public static boolean isReturn(int opcode) {
		return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
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
	public static List<AbstractInsnNode> getBasicBlocks(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks, boolean isPrecise) {

		Set<AbstractInsnNode> bb_begins = new HashSet<AbstractInsnNode>() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean add(AbstractInsnNode e) {
				return super.add(skipLabels(e, true));
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

			if (isPrecise && mightThrowException(instruction)) {
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
