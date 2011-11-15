package ch.usi.dag.disl.staticcontext;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class BasicBlockSC extends AbstractStaticContext {

	public int getTotBBs() {
        CtrlFlowGraph cfg = new CtrlFlowGraph(
                staticContextData.getMethodNode());
        return cfg.getNodes().size();
	}

	public int getBBSize() {

		int count = 1;
		AbstractInsnNode start;
		List<AbstractInsnNode> ends;

		start = staticContextData.getMarkedRegion().getStart();
		ends = staticContextData.getMarkedRegion().getEnds();

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
		return cfg.getIndex(staticContextData.getMarkedRegion().getStart());
	}
	
	public boolean isFirstOfLoop() {

		CtrlFlowGraph cfg = CtrlFlowGraph.build(staticContextData
				.getMethodNode());
		return cfg.getBB(staticContextData.getMarkedRegion().getStart())
				.isLoop();
	}
}
