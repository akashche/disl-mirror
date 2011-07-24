package ch.usi.dag.disl.staticinfo.analysis;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SEAnalysis extends AbstractStaticAnalysis{

	public int getBytecodeNumber() {

		return staticAnalysisInfo.getMarkedRegion().getStart().getOpcode();
	}

	public String getID() {

		AbstractInsnNode instruction = staticAnalysisInfo.getMarkedRegion()
				.getStart();

		if (instruction instanceof IincInsnNode) {
			return "I_" + ((IincInsnNode) instruction).var;
		}

		if (instruction instanceof VarInsnNode) {

			switch (instruction.getOpcode()) {
			case Opcodes.ILOAD:
			case Opcodes.ISTORE:
				return "I_" + ((VarInsnNode) instruction).var;
			case Opcodes.LLOAD:
			case Opcodes.LSTORE:
				return "L_" + ((VarInsnNode) instruction).var;
			case Opcodes.FLOAD:
			case Opcodes.FSTORE:
				return "F_" + ((VarInsnNode) instruction).var;
			case Opcodes.DLOAD:
			case Opcodes.DSTORE:
				return "D_" + ((VarInsnNode) instruction).var;
			case Opcodes.ALOAD:
			case Opcodes.ASTORE:
				return "A_" + ((VarInsnNode) instruction).var;
			default:
				break;
			}
		}

		return "null";
	}

	public int getIConst() {

		AbstractInsnNode instruction = staticAnalysisInfo.getMarkedRegion()
				.getStart();
		int opcode = instruction.getOpcode();

		switch (opcode) {
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
		case Opcodes.IINC:
			return ((IincInsnNode) instruction).var;
		default:
			return 0;
		}
	}
}
