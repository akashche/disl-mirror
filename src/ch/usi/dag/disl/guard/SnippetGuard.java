package ch.usi.dag.disl.guard;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;

public interface SnippetGuard {

	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion);
}
