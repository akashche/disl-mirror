package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.BasicBlockCalc;

public class BasicBlockMarker extends AbstractDWRMarker {

	protected boolean isPrecise = false;

	@Override
	public List<MarkedRegion> markWithDefaultWeavingReg(MethodNode methodNode) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		List<AbstractInsnNode> seperators = BasicBlockCalc.getAll(
				methodNode.instructions, methodNode.tryCatchBlocks, isPrecise);

		AbstractInsnNode last = AsmHelper.skipVirualInsns(
				methodNode.instructions.getLast(), false);

		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {

			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (i != seperators.size() - 2) {
				end = end.getPrevious();
			}

			regions.add(new MarkedRegion(start, AsmHelper.skipVirualInsns(end,
					false)));
		}

		return regions;
	}

}
