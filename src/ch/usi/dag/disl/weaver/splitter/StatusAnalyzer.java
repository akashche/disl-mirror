package ch.usi.dag.disl.weaver.splitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import ch.usi.dag.disl.util.cfg.BasicBlock;

public class StatusAnalyzer {

	private static StatusInterpreter interpreter = new StatusInterpreter();

	public Map<AbstractInsnNode, Frame<FlagValue>> frames;
	public Set<BasicBlock> visited;

	public StatusAnalyzer() {
		frames = new HashMap<AbstractInsnNode, Frame<FlagValue>>();
		visited = new HashSet<BasicBlock>();
	}

	public boolean merge(AbstractInsnNode instr, Frame<FlagValue> frame)
			throws AnalyzerException {

		Frame<FlagValue> origin = frames.get(instr);

		if (origin == null) {
			frames.put(instr, frame);
			return true;
		}

		return origin.merge(frame, interpreter);
	}

	public void analyze(Frame<FlagValue> frame, BasicBlock current,
			BasicBlock exit) throws AnalyzerException {

		if (current.getIndex() == exit.getIndex() + 1) {
			return;
		}

		visited.add(current);
		boolean changed = false;

		for (AbstractInsnNode instr : current) {

			frame = new Frame<FlagValue>(frame);
			frame.execute(instr, interpreter);
			changed = merge(instr, frame);
		}

		for (BasicBlock succ : current.getSuccessors()) {

			if (!visited.contains(succ) || changed) {
				analyze(frame, succ, exit);
			}
		}
	}

	public void analyze(MethodNode method, BasicBlock entrance,
			BasicBlock exit, Map<AbstractInsnNode, Frame<BasicValue>> mapping)
			throws AnalyzerException {

		Frame<FlagValue> frame = createFrame(
				mapping.get(entrance.getEntrance()), method);
		analyze(frame, entrance, exit);
	}

	public static Frame<FlagValue> createFrame(Frame<BasicValue> basic,
			MethodNode method) {

		Frame<FlagValue> frame = new Frame<FlagValue>(method.maxLocals,
				method.maxStack);

		for (int i = 0; i < frame.getLocals(); i++) {

			BasicValue value = basic.getLocal(i);

			if (value != null) {

				Type type = value.getType();

				if (type != null) {
					frame.setLocal(i, new FlagValue(type.getSize(), false));
				}
			}
		}

		return frame;
	}
}
