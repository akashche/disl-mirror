package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.MarkedRegion;

public class EmptyMarker implements Marker {
	
	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		return new LinkedList<MarkedRegion>();
	}
}
