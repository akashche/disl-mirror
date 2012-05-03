package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.marker.AbstractMarker.MarkedRegion;

public abstract class AbstractBranchSkipMarker {

	public List<MarkedRegion> mark(MethodNode methodNode) {
		
		List<MarkedRegion> mrs = markWithABSWithAAT(methodNode);
		
		// automatic branch skipping and after throw region computation
		for(MarkedRegion mr : mrs) {
			mr.computeAfterThrow(methodNode);
			mr.skipBranchesAtTheEnds();
		}
		
		return mrs;
	}
	
	/**
	 * Implementing this method will affect all marked regions.
	 *
	 * The regions will get automatic after throw computation
	 * The regions will get automatic branch skipping at the end
	 */
	public abstract List<MarkedRegion> markWithABSWithAAT(MethodNode methodNode);
}
