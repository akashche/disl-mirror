package ch.usi.dag.disl.util.stack;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class StackSimulator {

	private CtrlFlowGraph cfg;

	private Map<AbstractInsnNode, InstrStackState> instr2Stack;

	public StackSimulator(MethodNode method) throws DiSLException {
		cfg = new CtrlFlowGraph(method);
		instr2Stack = new HashMap<AbstractInsnNode, InstrStackState>();

		if (cfg.getNodes().size() == 0) {
			return;
		}

		cfg.visit(method.instructions.getFirst());

		BasicBlock bb = cfg.getNodes().get(0);
		InstrStackState state = new InstrStackState();
		visitBasicBlock(bb, state);
	}

	public void visitBasicBlock(BasicBlock bb, InstrStackState state)
			throws DiSLException {
		AbstractInsnNode instr = bb.getEntrance();

		do {
			if (instr.getOpcode() != -1) {
				InstrStackState current = instr2Stack.get(instr);

				if (current == null) {
					instr2Stack.put(instr, state.clone());
				} else if (!current.merge(state)) {
					return;
				}

				state.visit(instr);
			}

			instr = instr.getNext();
		} while (instr != bb.getExit().getNext());

		for (BasicBlock succossor : bb.getSuccessors()) {
			visitBasicBlock(succossor, state.clone());
		}
	}

	public StackEntry getStack(AbstractInsnNode current, int depth)
			throws DiSLException {

		InstrStackState state = instr2Stack.get(current);

		if (state == null) {
			throw new DiSLException("Instruction not included.");
		}

		int index = state.getEntries().size() - depth - 1;

		if (depth < 0 || index < 0) {
			throw new DiSLException("Illegal depth");
		}

		StackEntry entry = state.getEntries().get(index);

		if (entry == null) {
			throw new DiSLException("Stack overflow.");
		}

		return entry;
	}
}
