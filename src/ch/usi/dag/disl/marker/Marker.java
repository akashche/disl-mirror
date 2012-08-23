package ch.usi.dag.disl.marker;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;

/**
 * Basic interface that every marker has to implement. Marker should return
 * list of shadows that are marks for particular method.
 * 
 * Notice: It is not necessary to implement the interface directly. DiSL
 * contains several abstract classes to ease the development of new marker. 
 */
public interface Marker {
	
	/**
	 * Returns shadows for the marked method.
	 * 
	 * @param classNode represents class being marked
	 * @param methodNode represents method being marked
	 * @param snippet snippet defining the marker
	 * @return list of shadows for marked method
	 */
	public List<Shadow> mark(ClassNode classNode, MethodNode methodNode,
			Snippet snippet) throws MarkerException;
}
