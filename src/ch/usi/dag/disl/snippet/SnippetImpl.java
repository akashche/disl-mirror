package ch.usi.dag.disl.snippet;

import java.util.List;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;

public class SnippetImpl implements Snippet {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected InsnList asmCode;
	protected List<String> localVars;

	public SnippetImpl(Class<?> annotationClass, Marker marker, Scope scope,
			int order, InsnList asmCode, List<String> localVars) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.asmCode = asmCode;
		this.localVars = localVars;
	}

	public Class<?> getAnnotationClass() {
		return annotationClass;
	}

	public Marker getMarker() {
		return marker;
	}

	public Scope getScope() {
		return scope;
	}

	public int getOrder() {
		return order;
	}

	public InsnList getAsmCode() {
		return asmCode;
	}

	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}
}
