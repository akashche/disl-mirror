package ch.usi.dag.disl.staticcontext;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class BasicBlockStaticContext extends
		MethodAnalysisContext<CtrlFlowGraph> {

	public int getTotBBs() {
		return thisAnalysis.getNodes().size();
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
		return thisAnalysis.getIndex(staticContextData.getRegionStart());
	}

	@Override
	protected CtrlFlowGraph analysis() {
		return new CtrlFlowGraph(staticContextData.getMethodNode());
	}
}
