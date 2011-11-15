package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

public class EmptyMarker extends AbstractMarker {
	
	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		return new LinkedList<MarkedRegion>();
	}
}
