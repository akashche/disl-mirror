package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.InsnListHelper;

public class BasicBlockMarker implements Marker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		List<AbstractInsnNode> seperators = InsnListHelper
				.getBasicBlocks(method);		
		seperators.add(method.instructions.getLast());

		for (int i = 0; i < seperators.size() - 1; i++) {
			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1).getPrevious();
			
			if (start instanceof LabelNode){
				start = start.getNext();
			}

			if (start.getPrevious() != end && InsnListHelper.isBranch(end)){
				end = end.getPrevious();
			}

			regions.add(new MarkedRegion(method, start, end));
		}

		return regions;
	}

}
