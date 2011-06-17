package ch.usi.dag.disl.weaver;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

// The weaver instruments byte-codes into java class. 
public interface Weaver {

	// TODO include analysis
	void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings);
}
