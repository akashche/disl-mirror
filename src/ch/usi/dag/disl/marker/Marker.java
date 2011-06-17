package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.MethodNode;

public interface Marker {
	public List<MarkRegion> mark(MethodNode method);
}
