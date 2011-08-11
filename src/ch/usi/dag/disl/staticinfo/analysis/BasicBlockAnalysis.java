package ch.usi.dag.disl.staticinfo.analysis;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class BasicBlockAnalysis extends AbstractStaticAnalysis {

	public int getBBSize() {

		int count = 1;
		AbstractInsnNode start;
		List<AbstractInsnNode> ends;

		start = staticAnalysisData.getMarkedRegion().getStart();
		ends = staticAnalysisData.getMarkedRegion().getEnds();

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
				staticAnalysisData.getMethodNode());
		return cfg.getIndex(staticAnalysisData.getMarkedRegion().getStart());
	}
}
