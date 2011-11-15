package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.MarkedRegion;

public interface Marker {
	
	public List<MarkedRegion> mark(MethodNode method);
}
