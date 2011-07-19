package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.InsnListHelper;

public class BasicBlockMarker implements Marker {

	protected boolean isPrecise = false;

	@Override
	public List<MarkedRegion> mark(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		List<AbstractInsnNode> seperators = InsnListHelper.getBasicBlocks(
				method, isPrecise);

		AbstractInsnNode last = InsnListHelper.skipLabels(
				method.instructions.getLast(), false);

		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {

			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (end != last) {
				end = end.getPrevious();
			}

			regions.add(new MarkedRegion(method, InsnListHelper.skipLabels(
					start, true), InsnListHelper.skipLabels(end, false)));
		}

		return regions;
	}

}
