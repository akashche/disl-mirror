package ch.usi.dag.disl.util.stack;

import java.util.Stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

public class InstrStackState {

	private Stack<StackEntry> entries;

	public InstrStackState() {
		entries = new Stack<StackEntry>();
	}

	public InstrStackState clone() {

		InstrStackState state = new InstrStackState();

		for (StackEntry entry : entries) {
			state.entries.add(entry);
		}

		return state;
	}

	public boolean merge(InstrStackState state) {
		boolean flag = false; 

		for (int i = 0; i < Math.min(entries.size(), state.entries.size()); i++) {

			StackEntry entry = entries.get(i);
			StackEntry state_entry = state.entries.get(i);

			if (!(entry.getType() == state_entry.getType())) {
				throw new RuntimeException("Merge error");
			}

			flag = entry.merge(state_entry) | flag;
		}
		
		return flag;
	}

	public void visit(AbstractInsnNode instr) {
		switch (instr.getOpcode()) {

		case Opcodes.IALOAD:
		case Opcodes.IADD:
		case Opcodes.ISUB:
		case Opcodes.IMUL:
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.ISHL:
		case Opcodes.ISHR:
		case Opcodes.IUSHR:
		case Opcodes.IAND:
		case Opcodes.IOR:
		case Opcodes.IXOR:
		case Opcodes.LCMP:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			entries.pop();

		case Opcodes.INEG:
		case Opcodes.L2I:
		case Opcodes.F2I:
		case Opcodes.D2I:
		case Opcodes.INSTANCEOF:
		case Opcodes.ARRAYLENGTH:
			entries.pop();

		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.ILOAD:
		case Opcodes.JSR:
			entries.push(new StackEntry(Type.INT, instr));
			return;

		case Opcodes.LALOAD:
		case Opcodes.LADD:
		case Opcodes.LSUB:
		case Opcodes.LMUL:
		case Opcodes.LDIV:
		case Opcodes.LREM:
		case Opcodes.LSHL:
		case Opcodes.LSHR:
		case Opcodes.LUSHR:
		case Opcodes.LAND:
		case Opcodes.LOR:
		case Opcodes.LXOR:
			entries.pop();

		case Opcodes.LNEG:
		case Opcodes.I2L:
		case Opcodes.F2L:
		case Opcodes.D2L:
			entries.pop();

		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.LLOAD:
			entries.push(new StackEntry(Type.LONG, instr));
			return;

		case Opcodes.FALOAD:
		case Opcodes.FADD:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
		case Opcodes.FREM:
			entries.pop();

		case Opcodes.FNEG:
		case Opcodes.I2F:
		case Opcodes.L2F:
		case Opcodes.D2F:
			entries.pop();

		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.FLOAD:
			entries.push(new StackEntry(Type.FLOAT, instr));
			return;

		case Opcodes.DALOAD:
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			entries.pop();

		case Opcodes.DNEG:
		case Opcodes.I2D:
		case Opcodes.L2D:
		case Opcodes.F2D:
			entries.pop();

		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
		case Opcodes.DLOAD:
			entries.push(new StackEntry(Type.DOUBLE, instr));
			return;

		case Opcodes.AALOAD:
			entries.pop();

		case Opcodes.CHECKCAST:
		case Opcodes.NEWARRAY:
		case Opcodes.ANEWARRAY:
			entries.pop();

		case Opcodes.ACONST_NULL:
		case Opcodes.ALOAD:
		case Opcodes.NEW:
			entries.push(new StackEntry(Type.OBJECT, instr));
			return;

		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
			entries.pop();

		case Opcodes.I2B:
		case Opcodes.I2C:
			entries.pop();

		case Opcodes.BIPUSH:
			entries.push(new StackEntry(Type.CHAR, instr));
			return;

		case Opcodes.SALOAD:
			entries.pop();

		case Opcodes.I2S:
			entries.pop();

		case Opcodes.SIPUSH:
			entries.push(new StackEntry(Type.SHORT, instr));
			return;

		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			entries.pop();

		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
		case Opcodes.PUTFIELD:
			entries.pop();

		case Opcodes.ISTORE:
		case Opcodes.LSTORE:
		case Opcodes.FSTORE:
		case Opcodes.DSTORE:
		case Opcodes.ASTORE:
		case Opcodes.ATHROW:
		case Opcodes.POP:
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.TABLESWITCH:
		case Opcodes.LOOKUPSWITCH:
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
		case Opcodes.PUTSTATIC:
			entries.pop();
			return;

		case Opcodes.SWAP: {
			StackEntry top = entries.pop();
			StackEntry bottom = entries.pop();
			entries.push(top);
			entries.push(bottom);
			return;
		}

		case Opcodes.MULTIANEWARRAY: {
			int n = ((MultiANewArrayInsnNode) instr).dims;

			for (int i = 0; i < n; i++) {
				entries.pop();
			}

			entries.push(new StackEntry(Type.OBJECT, instr));
			return;
		}

		case Opcodes.POP2: {
			StackEntry entry = entries.pop();

			if (entry.getType() != Type.DOUBLE && entry.getType() != Type.LONG) {
				entries.pop();
			}

			return;
		}

		case Opcodes.DUP: {
			StackEntry entry = entries.pop();
			entries.push(entry);
			entries.push(entry.clone());
			return;
		}

		case Opcodes.DUP_X1: {
			StackEntry entry_1 = entries.pop();
			StackEntry entry_2 = entries.pop();

			entries.push(entry_1);
			entries.push(entry_2);
			entries.push(entry_1.clone());
			return;
		}

		case Opcodes.DUP_X2: {
			StackEntry entry_1 = entries.pop();
			StackEntry entry_2 = entries.pop();
			StackEntry entry_3 = entries.pop();

			entries.push(entry_1);
			entries.push(entry_3);
			entries.push(entry_2);
			entries.push(entry_1.clone());
			return;
		}

		case Opcodes.DUP2: {
			StackEntry entry_1 = entries.pop();

			if (entry_1.getType() == Type.DOUBLE || entry_1.getType() == Type.LONG) {

				entries.push(entry_1);
				entries.push(entry_1.clone());
				return;
			}

			StackEntry entry_2 = entries.pop();

			entries.push(entry_2);
			entries.push(entry_1);
			entries.push(entry_2.clone());
			entries.push(entry_1.clone());
			return;
		}

		case Opcodes.DUP2_X1: {
			StackEntry entry_1 = entries.pop();

			if (entry_1.getType() == Type.DOUBLE || entry_1.getType() == Type.LONG) {

				StackEntry entry_3 = entries.pop();
				entries.push(entry_1);
				entries.push(entry_3);
				entries.push(entry_1.clone());
				return;
			}

			StackEntry entry_2 = entries.pop();
			StackEntry entry_3 = entries.pop();

			entries.push(entry_2);
			entries.push(entry_1);
			entries.push(entry_3);
			entries.push(entry_2.clone());
			entries.push(entry_1.clone());
			return;
		}

		case Opcodes.DUP2_X2: {
			StackEntry entry_1 = entries.pop();

			if (entry_1.getType() == Type.DOUBLE || entry_1.getType() == Type.LONG) {

				StackEntry entry_3 = entries.pop();

				if (entry_3.getType() == Type.DOUBLE || entry_3.getType() == Type.LONG) {

					entries.push(entry_1);
					entries.push(entry_3);
					entries.push(entry_1.clone());
				}

				StackEntry entry_4 = entries.pop();
				entries.push(entry_1);
				entries.push(entry_4);
				entries.push(entry_3);
				entries.push(entry_1.clone());
				return;
			}

			StackEntry entry_2 = entries.pop();
			StackEntry entry_3 = entries.pop();

			if (entry_1.getType() == Type.DOUBLE || entry_1.getType() == Type.LONG) {

				entries.push(entry_2);
				entries.push(entry_1);
				entries.push(entry_3);
				entries.push(entry_2.clone());
				entries.push(entry_1.clone());
			}

			StackEntry entry_4 = entries.pop();
			entries.push(entry_2);
			entries.push(entry_1);
			entries.push(entry_4);
			entries.push(entry_3);
			entries.push(entry_2.clone());
			entries.push(entry_1.clone());

			return;
		}
		case Opcodes.LDC: {
			Object obj = ((LdcInsnNode) instr).cst;

			if (obj instanceof Integer) {
				entries.push(new StackEntry(Type.INT, instr));
			} else if (obj instanceof Float) {
				entries.push(new StackEntry(Type.FLOAT, instr));
			} else if (obj instanceof String) {
				entries.push(new StackEntry(Type.OBJECT, instr));
			} else if (obj instanceof Long) {
				entries.push(new StackEntry(Type.LONG, instr));
			} else if (obj instanceof Double) {
				entries.push(new StackEntry(Type.DOUBLE, instr));
			}

			return;
		}

		case Opcodes.GETFIELD:
			entries.pop();

		case Opcodes.GETSTATIC: {
			int type = Type.getType(((FieldInsnNode) instr).desc).getSort();
			entries.push(new StackEntry(type, instr));
			return;
		}

		case Opcodes.INVOKEDYNAMIC:
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKESPECIAL:
			entries.pop();

		case Opcodes.INVOKESTATIC: {
			int count = Type.getArgumentTypes(((MethodInsnNode) instr).desc).length;

			for (int i = 0; i < count; i++) {
				entries.pop();
			}

			int type = Type.getReturnType(((FieldInsnNode) instr).desc)
					.getSort();

			if (type != Type.VOID) {
				entries.push(new StackEntry(type, instr));
			}

			return;
		}

		case Opcodes.IINC:
		case Opcodes.GOTO:
		case Opcodes.RET:
		case Opcodes.RETURN:
		default:
			return;
		}
	}

	public Stack<StackEntry> getEntries() {
		return entries;
	}
}
