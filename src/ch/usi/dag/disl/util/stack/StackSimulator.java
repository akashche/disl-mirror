package ch.usi.dag.disl.util.stack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class StackSimulator {

	private CtrlFlowGraph cfg;

	private Map<AbstractInsnNode, InstrStackState> instr2Stack;

	public StackSimulator(MethodNode method) {
		cfg = new CtrlFlowGraph(method);
		instr2Stack = new HashMap<AbstractInsnNode, InstrStackState>();

		if (cfg.getNodes().size() == 0) {
			return;
		}

		BasicBlock bb = cfg.getNodes().get(0);
		InstrStackState state = new InstrStackState();
		visitBasicBlock(bb, state);
	}

	public void visitBasicBlock(BasicBlock bb, InstrStackState state) {
		AbstractInsnNode instr = bb.getEntrance();

		do {
			InstrStackState current = instr2Stack.get(instr);

			if (current == null) {
				instr2Stack.put(instr, state.clone());
			} else if (!current.merge(state)) {
				return;
			}

			state.visit(instr);
			instr = instr.getNext();
		} while (instr != bb.getExit());

		for (BasicBlock succossor : bb.getSuccessors()) {
			visitBasicBlock(succossor, state);
		}
	}

	public Set<AbstractInsnNode> getStack(AbstractInsnNode current, int depth) {

		InstrStackState state = instr2Stack.get(current);

		int index = state.getEntries().size() - depth - 1;

		if (depth < 0 || index < 0) {
			return null;
		}

		return state.getEntries().get(index).getList();
	}
}
