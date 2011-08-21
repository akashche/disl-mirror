package ch.usi.dag.disl.util.cfg;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.stack.StackUtil;

public class Slicer {

	public static class Field {
		public String owner;
		public String name;

		public Field(String owner, String name) {
			this.owner = owner;
			this.name = name;
		}
	}

	private InsnList instructions;
	private int size;

	private CtrlFlowGraph cfg;
	private Frame<SourceValue>[] frames;

	public Slicer(String className, MethodNode method) throws AnalyzerException {

		instructions = method.instructions;
		size = instructions.size();

		cfg = new CtrlFlowGraph(method);
		cfg.build();

		frames = StackUtil.getSourceAnalyzer().analyze(className, method);
	}

	public boolean[] backward(List<Field> fields, AbstractInsnNode from) {
		boolean[] flags = new boolean[size];

		for (int index = 0; index < size; index++) {
			flags[index] = instructions.get(index) instanceof LabelNode;
		}

		from = AsmHelper.skipLabels(from, false);

		List<AbstractInsnNode> ilist = new LinkedList<AbstractInsnNode>();

		for (Field field : fields) {
			lastStore(flags, ilist, from, field.owner, field.name);
		}

		for (BasicBlock bb : cfg.getNodes()) {

			AbstractInsnNode exit = bb.getExit();

			if (AsmHelper.isBranch(exit)) {
				setFlag(flags, ilist, exit);
			}
		}

		while (ilist.size() != 0) {

			AbstractInsnNode instr = ilist.get(0);
			ilist.remove(0);
			int index = instructions.indexOf(instr);

			for (int i = 0; i < frames[index].getStackSize(); i++) {
				setFlag(flags, ilist, frames[index].getStack(i).insns);
			}

			switch (instr.getOpcode()) {

			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:

				lastStore(flags, ilist, instr, instr);
				break;

			case Opcodes.GETSTATIC:
			case Opcodes.GETFIELD:

				lastStore(flags, ilist, instr, instr);
				break;

			default:
				break;
			}
		}

		return flags;
	}

	private void setFlag(boolean[] flags, List<AbstractInsnNode> ilist,
			AbstractInsnNode instr) {

		int index = instructions.indexOf(instr);

		if (flags[index]) {
			return;
		}

		flags[index] = true;
		ilist.add(instr);
	}

	private void setFlag(boolean[] flags, List<AbstractInsnNode> ilist,
			Collection<AbstractInsnNode> instrs) {

		for (AbstractInsnNode instr : instrs) {
			setFlag(flags, ilist, instr);
		}
	}

	private void lastStore(boolean[] flags, List<AbstractInsnNode> ilist,
			AbstractInsnNode from, String owner, String name) {

		Set<AbstractInsnNode> last = cfg.lastStore(from, new FieldInsnNode(
				Opcodes.PUTSTATIC, owner, name, null));

		for (AbstractInsnNode instr : last) {
			setFlag(flags, ilist, instr);
		}
	}

	private void lastStore(boolean[] flags, List<AbstractInsnNode> ilist,
			AbstractInsnNode from, AbstractInsnNode criterion) {

		Set<AbstractInsnNode> last = cfg.lastStore(from, criterion);

		for (AbstractInsnNode instr : last) {
			setFlag(flags, ilist, instr);
		}
	}
}
