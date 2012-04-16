package ch.usi.dag.disl.weaver.splitter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class SuperBlock implements Comparable<SuperBlock>,
		Iterable<AbstractInsnNode> {

	public static final Set<String> wrappers = new HashSet<String>();

	static {
		wrappers.add("java.lang.Boolean");
		wrappers.add("java.lang.Byte");
		wrappers.add("java.lang.Character");
		wrappers.add("java.lang.Double");
		wrappers.add("java.lang.Float");
		wrappers.add("java.lang.Integer");
		wrappers.add("java.lang.Long");
		wrappers.add("java.lang.Short");
		wrappers.add("java.lang.String");
	}

	public int index;
	public AbstractInsnNode start;
	public AbstractInsnNode end;

	public Set<LocalVar> usedVar;

	public int size;
	public int parSize;

	public boolean crossed;

	public Frame<FlagValue> frame;
	public Map<AbstractInsnNode, Frame<BasicValue>> mapping;

	public SuperBlock(int index, BasicBlock entrance, BasicBlock exit,
			LocalVar[] locals, MethodNode method,
			Map<AbstractInsnNode, Frame<BasicValue>> mapping) {

		this.index = index;
		start = entrance.getEntrance();
		end = exit.getExit();

		this.mapping = mapping;

		usedVar = new HashSet<LocalVar>();
		size = SplitterHelper.getSize(this);
		crossed = false;

		try {

			StatusAnalyzer analyzer = new StatusAnalyzer();
			analyzer.analyze(method, entrance, exit, mapping);
			frame = analyzer.frames.get(end);
		} catch (Exception e) {
			frame = new Frame<FlagValue>(method.maxLocals, method.maxStack);
		}

		initUsedVar(locals);
	}

	private void initUsedVar(LocalVar[] locals) {

		LocalVar var;

		for (AbstractInsnNode instr : this) {

			switch (instr.getOpcode()) {
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
				var = locals[((VarInsnNode) instr).var];
				var.addUse(this);
				usedVar.add(var);
				break;

			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
			case Opcodes.ASTORE:
				var = locals[((VarInsnNode) instr).var];
				var.addDefine(this);
				usedVar.add(var);
				break;

			case Opcodes.IINC:
				var = locals[((IincInsnNode) instr).var];
				var.addDefine(this);
				usedVar.add(var);
				break;

			default:
				break;
			}
		}

		parSize = 0;

		for (LocalVar iter : usedVar) {

			if (isParameter(iter)) {
				parSize++;
			}
		}
	}

	public boolean isParameter(LocalVar var) {

		FlagValue value = frame.getLocal(var.index);

		if (value == null) {
			return true;
		} else {
			return !value.getFlag();
		}
	}

	public boolean needReturn(LocalVar var) {

		BasicValue value = mapping.get(start).getLocal(var.index);
		Type type = null;

		if (value != null) {
			type = value.getType();
		}

		if (type != null) {

			if (type.getSort() == Type.ARRAY) {
				return false;
			}

			if (type.getSort() == Type.OBJECT) {
				if (!wrappers.contains(type.getClassName())) {
					return false;
				}
			}
		}

		for (SuperBlock sb : var.uses) {
			if ((sb.index > index) && sb.isParameter(var)) {
				return true;
			}
		}

		return false;
	}

	public boolean hasMultipleReturnLocal() {

		int count = 0;

		for (LocalVar var : usedVar) {
			if (needReturn(var)) {
				count++;
			}
		}

		return count >= 2;
	}

	public void containsTryBlock(List<TryCatchBlockNode> tcbs) {

		for (TryCatchBlockNode tcb : tcbs) {

			AbstractInsnNode instr = tcb.start;

			while (instr != null && !instr.equals(start)) {

				if (instr.equals(end)) {
					crossed = true;
					return;
				}

				instr = instr.getNext();
			}

			instr = tcb.end;

			while (instr != null && !instr.equals(end)) {

				if (instr.equals(start)) {
					crossed = true;
					return;
				}

				instr = instr.getPrevious();
			}
		}
	}

	public boolean splitable() {
		return !crossed && !hasMultipleReturnLocal();
	}

	public int getWeight() {
		return parSize * 1000 / size;
	}

	public static List<SuperBlock> build(MethodNode method, CtrlFlowGraph cfg,
			LocalVar[] locals, Map<AbstractInsnNode, Frame<BasicValue>> mapping) {

		List<SuperBlock> blocks = new LinkedList<SuperBlock>();

		List<BasicBlock> nodes = cfg.getNodes();

		Set<Integer> entrance = new HashSet<Integer>();
		Set<Integer> exit = new HashSet<Integer>();

		BasicBlock start = null;
		BasicBlock current = null;

		int index = 0;

		for (int i = 0; i < nodes.size(); i++) {

			current = nodes.get(i);

			if (start == null) {
				start = current;
			}

			if (current.getPredecessors().size() == 0) {
				entrance.add(-1);
			} else {

				for (BasicBlock predecessor : current.getPredecessors()) {

					int pre = predecessor.getIndex();

					if (pre > i || pre < start.getIndex()) {
						entrance.add(pre);
					}
				}
			}

			if (current.getSuccessors().size() == 0) {
				exit.add(-1);
			} else {

				for (BasicBlock successor : current.getSuccessors()) {

					int succ = successor.getIndex();

					if (succ > i) {
						exit.add(succ);
					}
				}
			}

			entrance.remove(i);
			exit.remove(i);

			if (entrance.size() > 1 || exit.size() > 1) {
				continue;
			}

			int next = exit.iterator().next();

			if (next != -1 && next != i + 1) {
				continue;
			}

			AbstractInsnNode next_instr = current.getExit().getNext();

			if (next_instr != null) {

				Frame<BasicValue> frame = mapping.get(next_instr);

				if (frame.getStackSize() > 0) {
					continue;
				}
			}

			SuperBlock sb = new SuperBlock(index++, start, current, locals,
					method, mapping);
			blocks.add(sb);

			start = null;
			entrance = new HashSet<Integer>();
			exit = new HashSet<Integer>();

		}

		return blocks;
	}

	@Override
	public int compareTo(SuperBlock thas) {
		return getWeight() - thas.getWeight();
	}

	class SuperBlockIterator implements Iterator<AbstractInsnNode> {

		private AbstractInsnNode current;

		public SuperBlockIterator() {
			current = start;
		}

		@Override
		public boolean hasNext() {
			return current != end.getNext();
		}

		@Override
		public AbstractInsnNode next() {
			AbstractInsnNode temp = current;
			current = current.getNext();
			return temp;
		}

		@Override
		public void remove() {
			throw new DiSLFatalException("Readonly iterator.");
		}

	}

	@Override
	public Iterator<AbstractInsnNode> iterator() {

		return new SuperBlockIterator();
	}

}
