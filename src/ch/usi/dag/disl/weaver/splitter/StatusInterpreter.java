package ch.usi.dag.disl.weaver.splitter;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Interpreter;

public class StatusInterpreter extends Interpreter<FlagValue> {

	protected StatusInterpreter() {
		super(Opcodes.ASM4);
	}

	@Override
	public FlagValue newValue(final Type type) {
		if (type == Type.VOID_TYPE) {
			return null;
		}

		return new FlagValue(type == null ? 1 : type.getSize(), false);
	}

	@Override
	public FlagValue newOperation(final AbstractInsnNode insn) {

		switch (insn.getOpcode()) {
		case Opcodes.ACONST_NULL:
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			return new FlagValue(1, true);

		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			return new FlagValue(2, true);

		case Opcodes.LDC:
			Object cst = ((LdcInsnNode) insn).cst;
			return new FlagValue(
					cst instanceof Long || cst instanceof Double ? 2 : 1, true);

		case Opcodes.GETSTATIC:
			return new FlagValue(Type.getType(((FieldInsnNode) insn).desc)
					.getSize(), false);

		case Opcodes.NEW:
			return new FlagValue(1);

		default:
			return new FlagValue(1, false);
		}
	}

	@Override
	public FlagValue copyOperation(final AbstractInsnNode insn,
			final FlagValue value) {
		return value.clone();
	}

	@Override
	public FlagValue unaryOperation(final AbstractInsnNode insn,
			final FlagValue value) {

		if (!value.getFlag()) {

			switch (insn.getOpcode()) {
			case Opcodes.LNEG:
			case Opcodes.DNEG:
			case Opcodes.I2L:
			case Opcodes.I2D:
			case Opcodes.L2D:
			case Opcodes.F2L:
			case Opcodes.F2D:
			case Opcodes.D2L:
				return new FlagValue(2, false);

			case Opcodes.GETFIELD:
				return new FlagValue(Type.getType(((FieldInsnNode) insn).desc)
						.getSize(), false);

			default:
				return new FlagValue(1, false);
			}
		}

		switch (insn.getOpcode()) {

		case Opcodes.INEG:
		case Opcodes.FNEG:
		case Opcodes.IINC:
		case Opcodes.I2F:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.F2I:
		case Opcodes.D2I:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
		case Opcodes.CHECKCAST:
		case Opcodes.INSTANCEOF:
			return new FlagValue(1, true);

		case Opcodes.LNEG:
		case Opcodes.DNEG:
		case Opcodes.I2L:
		case Opcodes.I2D:
		case Opcodes.L2D:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2L:
			return new FlagValue(2, true);

		default:
			return new FlagValue(1, false);
		}
	}

	@Override
	public FlagValue binaryOperation(final AbstractInsnNode insn,
			final FlagValue value1, final FlagValue value2) {

		if (!(value1.getFlag() & value2.getFlag())) {

			switch (insn.getOpcode()) {
			case Opcodes.LALOAD:
			case Opcodes.DALOAD:
			case Opcodes.LADD:
			case Opcodes.DADD:
			case Opcodes.LSUB:
			case Opcodes.DSUB:
			case Opcodes.LMUL:
			case Opcodes.DMUL:
			case Opcodes.LDIV:
			case Opcodes.DDIV:
			case Opcodes.LREM:
			case Opcodes.DREM:
			case Opcodes.LSHL:
			case Opcodes.LSHR:
			case Opcodes.LUSHR:
			case Opcodes.LAND:
			case Opcodes.LOR:
			case Opcodes.LXOR:
				return new FlagValue(2, false);

			default:
				return new FlagValue(1, false);
			}
		}

		switch (insn.getOpcode()) {

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
		case Opcodes.FADD:
		case Opcodes.FSUB:
		case Opcodes.FMUL:
		case Opcodes.FDIV:
		case Opcodes.FREM:
		case Opcodes.LCMP:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
			return new FlagValue(1, true);

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
		case Opcodes.DADD:
		case Opcodes.DSUB:
		case Opcodes.DMUL:
		case Opcodes.DDIV:
		case Opcodes.DREM:
			return new FlagValue(2, true);

		case Opcodes.LALOAD:
		case Opcodes.DALOAD:
			return new FlagValue(2, false);

		default:
			return new FlagValue(1, false);
		}
	}

	@Override
	public FlagValue ternaryOperation(final AbstractInsnNode insn,
			final FlagValue value1, final FlagValue value2,
			final FlagValue value3) {
		return new FlagValue(1, false);
	}

	@Override
	public FlagValue naryOperation(final AbstractInsnNode insn,
			final List<? extends FlagValue> values) {

		int opcode = insn.getOpcode();

		if (opcode == Opcodes.MULTIANEWARRAY) {
			return new FlagValue(1, false);
		} else if (opcode == Opcodes.INVOKEDYNAMIC) {
			return new FlagValue(Type.getReturnType(
					((InvokeDynamicInsnNode) insn).desc).getSize(), false);
		} else {

			MethodInsnNode min = (MethodInsnNode) insn;
			int size = Type.getReturnType(min.desc).getSize();

			if ("<init>".equals(min.name)) {

				FlagValue _this = values.get(0);

				for (int i = 1; i < values.size(); i++) {
					if (!values.get(i).getFlag()) {
						return new FlagValue(size, false);
					}
				}

				_this.setFlag(true);
				return _this.clone();
			}

			for (FlagValue value : values) {
				if (!value.getFlag()) {
					return new FlagValue(size, false);
				}
			}

			return new FlagValue(size, true);
		}

	}

	@Override
	public void returnOperation(final AbstractInsnNode insn,
			final FlagValue value, final FlagValue expected) {
	}

	@Override
	public FlagValue merge(final FlagValue d, final FlagValue w) {

		if ((d.getSize() == w.getSize()) && (d.getFlag() == w.getFlag())) {
			return d;
		}

		return new FlagValue(Math.min(d.getSize(), w.getSize()), d.getFlag()
				&& w.getFlag());
	}

}
