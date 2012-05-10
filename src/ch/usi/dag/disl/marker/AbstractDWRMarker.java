package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.MethodNode;

public abstract class AbstractDWRMarker extends AbstractMarker{

	public final List<MarkedRegion> mark(MethodNode methodNode) {

		List<MarkedRegion> mrs = markWithDefaultWeavingReg(methodNode);

		// automatically compute default weaving region
		for (MarkedRegion mr : mrs) {
			mr.setWeavingRegion(mr.computeDefaultWeavingRegion(methodNode));
		}

		return mrs;
	}
	
	/**
	 * Implementing this method will affect all marked regions.
	 *
	 * The regions will get automatic after throw computation
	 * The regions will get automatic branch skipping at the end
	 */
	public abstract List<MarkedRegion> markWithDefaultWeavingReg(
			MethodNode methodNode);
}
