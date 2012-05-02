package ch.usi.dag.disl.staticcontext;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.staticcontext.customdatacache.MethodCDCache;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class BasicBlockStaticContext extends
		MethodCDCache<CtrlFlowGraph> {

	public int getTotBBs() {
		return customData.getNodes().size();
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
		return customData.getIndex(staticContextData.getRegionStart());
	}

	@Override
	protected CtrlFlowGraph produceCustomData() {
		return new CtrlFlowGraph(staticContextData.getMethodNode());
	}
}
