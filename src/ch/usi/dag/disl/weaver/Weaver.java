package ch.usi.dag.disl.weaver;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// TODO include analysis
	// TODO support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings) {
		
		// TODO! implement
	}
}
