package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;

public interface Marker {
	
	public List<Shadow> mark(ClassNode classNode, MethodNode methodNode,
			Snippet snippet) throws MarkerException;
}
