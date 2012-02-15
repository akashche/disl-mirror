package ch.usi.dag.disl.util.cfg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class LoopAnalyzer {

	// compute dominators
	private static Map<BasicBlock, Set<BasicBlock>> computeDominators(
			CtrlFlowGraph cfg, MethodNode method) {

		// initialization
		Set<BasicBlock> entries = new HashSet<BasicBlock>();
		entries.add(cfg.getBB(method.instructions.getFirst()));

		for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
			entries.add(cfg.getBB(tcb.handler));
		}

		Map<BasicBlock, Set<BasicBlock>> dominatormapping = new HashMap<BasicBlock, Set<BasicBlock>>();

		for (BasicBlock bb : cfg.getNodes()) {

			Set<BasicBlock> dominators = new HashSet<BasicBlock>();

			if (entries.contains(bb)) {
				dominators.add(bb);
			} else {
				dominators.addAll(cfg.getNodes());
			}

			dominatormapping.put(bb, dominators);
		}

		// whether the dominators of any basic block is changed
		boolean changed;

		// loop until no more changes
		do {
			changed = false;

			for (BasicBlock bb : cfg.getNodes()) {

				if (entries.contains(bb)) {
					continue;
				}

				Set<BasicBlock> dominators = dominatormapping.get(bb);
				dominators.remove(bb);

				// update the dominators of current basic block,
				// contains only the dominators of its predecessors
				for (BasicBlock predecessor : bb.getPredecessors()) {

					if (dominators.retainAll(dominatormapping.get(predecessor))) {
						changed = true;
					}
				}

				dominators.add(bb);
			}
		} while (changed);

		return dominatormapping;
	}

	public static boolean isEntryOfLoop(MethodNode method,
			AbstractInsnNode instr) {

		CtrlFlowGraph cfg = CtrlFlowGraph.build(method);

		cfg.visit(method.instructions.getFirst());
		Map<BasicBlock, Set<BasicBlock>> dominatormapping = computeDominators(
				cfg, method);

		BasicBlock entry = cfg.getBB(instr);

		for (BasicBlock bb : entry.getPredecessors()) {
			if (dominatormapping.get(bb).contains(entry)) {
				return true;
			}
		}

		return false;
	}

}
