package ch.usi.dag.disl.weaver.splitter;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public class SplitterHelper {

	public static boolean addReturn(InsnList iList) {

		for (AbstractInsnNode instr : iList.toArray()) {

			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {

				return false;
			}
		}

		AbstractInsnNode end = iList.getLast();
		iList.insert(end, new InsnNode(Opcodes.RETURN));
		return true;
	}

	public static AbstractInsnNode getReturn(InsnList iList) {

		List<AbstractInsnNode> returns = new LinkedList<>();

		for (AbstractInsnNode instr : iList.toArray()) {

			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {

				returns.add(instr);
			}
		}

		if (returns.size() == 1) {
			return returns.get(0);
		} else {

			LabelNode label = new LabelNode();
			AbstractInsnNode newReturn = new InsnNode(returns.get(0)
					.getOpcode());

			iList.add(label);
			iList.add(newReturn);

			for (AbstractInsnNode instr : returns) {

				if (instr.getNext() != label) {
					iList.insert(instr, new JumpInsnNode(Opcodes.GOTO, label));
				}

				iList.remove(instr);
			}

			return newReturn;
		}
	}

	// http://en.wikipedia.org/wiki/Java_bytecode_instruction_listings
	public static int getSize(AbstractInsnNode instr) {

		switch (instr.getOpcode()) {

		case Opcodes.ALOAD:
		case Opcodes.ASTORE:
		case Opcodes.BIPUSH:
		case Opcodes.DLOAD:
		case Opcodes.DSTORE:
		case Opcodes.FLOAD:
		case Opcodes.FSTORE:
		case Opcodes.ILOAD:
		case Opcodes.ISTORE:
		case Opcodes.LDC: // LDC_W & LDC2_W takes 3 byte.
		case Opcodes.LLOAD:
		case Opcodes.LSTORE:
		case Opcodes.NEWARRAY:
		case Opcodes.RET:

			return 2;

		case Opcodes.ANEWARRAY:
		case Opcodes.CHECKCAST:
		case Opcodes.GETFIELD:
		case Opcodes.GETSTATIC:
		case Opcodes.GOTO: // GOTO_W takes 5 byte.
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IFEQ:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IFLT:
		case Opcodes.IFNE:
		case Opcodes.IFNONNULL:
		case Opcodes.IFNULL:
		case Opcodes.IINC:
		case Opcodes.INSTANCEOF:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKESTATIC:
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.JSR: // JSR_W takes 5 byte.
		case Opcodes.NEW:
		case Opcodes.PUTFIELD:
		case Opcodes.PUTSTATIC:
		case Opcodes.SIPUSH:

			return 3;

		case Opcodes.MULTIANEWARRAY:

			return 4;

		case Opcodes.INVOKEDYNAMIC:
		case Opcodes.INVOKEINTERFACE:

			return 5;

		case Opcodes.LOOKUPSWITCH:
		case Opcodes.TABLESWITCH:

			return 5; // 4+

		default:
			return 1;

		}
	}

	public static int getSize(SuperBlock sb) {

		int total = 0;

		for (AbstractInsnNode iter : sb) {
			total += getSize(iter);
		}

		return total;
	}

}
