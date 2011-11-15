package ch.usi.dag.disl.guard;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;

public interface SnippetGuard {

	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion);
}
