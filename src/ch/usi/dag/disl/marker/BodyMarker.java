package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

public class BodyMarker implements Marker {

	@Override
	public List<MarkRegion> mark(MethodNode method) {
		List<MarkRegion> regions = new LinkedList<MarkRegion>();
		InsnList ilst = method.instructions;
		MarkRegion region = new MarkRegion();
		region.start = ilst.getFirst();
		// FIXME Does the region contain the return-instruction?
		// What about a try-finally block?
		region.end = ilst.getLast();
		regions.add(region);
		return regions;
	}

}
