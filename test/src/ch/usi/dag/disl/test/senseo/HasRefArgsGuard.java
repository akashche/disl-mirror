package ch.usi.dag.disl.test.senseo;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.guard.SnippetGuard;

// check if the method has at least one object reference 
public class HasRefArgsGuard implements SnippetGuard {
	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		// if any argument has a reference "L....;" then return true
		if(methodNode.desc.substring(1, methodNode.desc.indexOf(')')).contains("L"))
			return true;
		return false;
	}
}
