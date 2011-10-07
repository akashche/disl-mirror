package ch.usi.dag.disl.dislclass.snippet.marker;

import java.util.List;

import org.objectweb.asm.tree.MethodNode;

public interface Marker {
	
	// TODO ! shadow - pass instruction list
	public List<MarkedRegion> mark(MethodNode method);
}
