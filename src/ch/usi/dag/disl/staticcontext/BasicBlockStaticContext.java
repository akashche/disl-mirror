package ch.usi.dag.disl.staticcontext;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;
import ch.usi.dag.disl.util.cfg.LoopAnalyzer;

public class BasicBlockStaticContext extends AbstractStaticContext {

	public int getTotBBs() {
		CtrlFlowGraph cfg = new CtrlFlowGraph(
				staticContextData.getMethodNode());
		return cfg.getNodes().size();
	}

	public int getBBSize() {

		int count = 1;
		AbstractInsnNode start;
		List<AbstractInsnNode> ends;

		start = staticContextData.getRegionStart();
		ends = staticContextData.getRegionEnds();

		while (!ends.contains(start)) {

			if (start.getOpcode() != 1) {
				count++;
			}

			start = start.getNext();
		}

		return count;
	}

	public int getBBindex() {

		CtrlFlowGraph cfg = new CtrlFlowGraph(
				staticContextData.getMethodNode());
		return cfg.getIndex(staticContextData.getRegionStart());
	}

	public boolean isFirstOfLoop() {

		return LoopAnalyzer.isEntryOfLoop(staticContextData.getMethodNode(),
				staticContextData.getRegionStart());
	}
}
