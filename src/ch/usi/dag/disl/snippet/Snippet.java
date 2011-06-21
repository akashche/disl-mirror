package ch.usi.dag.disl.snippet;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;

public interface Snippet extends Comparable<Snippet> {

	public Class<?> getAnnotationClass();

	public Marker getMarker();
	
	public Scope getScope();

	public int getOrder();
	
	public InsnList getAsmCode();
	
}
