package ch.usi.dag.disl.viewer;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;

public interface Viewer {

	// TODO include analysis
	void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings);
}
