package ch.usi.dag.disl.snippet;

import java.util.Set;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.staticinfo.analysis.Analysis;

public interface Snippet extends Comparable<Snippet> {

	public Class<?> getAnnotationClass();

	public Marker getMarker();
	
	public Scope getScope();

	public int getOrder();
	
	public InsnList getAsmCode();
	
	public Set<String> getLocalVars();
	
	public Set<Class<? extends Analysis>> getAnalyses();

}
