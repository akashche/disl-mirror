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
		List<AbstractInsnNode> seperators = InsnListHelper
				.getBasicBlocks(method);

		AbstractInsnNode last = method.instructions.getLast();
		
		while (last.getOpcode() == -1) {
			last = last.getPrevious();
		}
		
		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {
			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (end != last) {
				end = end.getPrevious();
			}

			while (start.getOpcode() == -1) {
				start = start.getNext();
			}

			regions.add(new MarkedRegion(method, start, end));
		}

		return regions;
	}

}
