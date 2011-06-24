package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.InsnListHelper;

public class BasicBlockMarker implements Marker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		List<AbstractInsnNode> bb_begins = InsnListHelper
				.getBasicBlocks(method);
		AbstractInsnNode start, end;

		for (int i = 0; i < bb_begins.size() - 1; i++) {
			start = bb_begins.get(i);
			end = bb_begins.get(i + 1);

			if (InsnListHelper.isBranch(end))
				end = end.getPrevious();

			regions.add(new MarkedRegion(method, start, end));
		}

		return regions;
	}

}
