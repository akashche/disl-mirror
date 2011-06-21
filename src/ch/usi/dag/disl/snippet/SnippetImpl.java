package ch.usi.dag.disl.snippet;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;

public class SnippetImpl implements Snippet {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected InsnList asmCode;

	public SnippetImpl(Class<?> annotationClass, Marker marker, Scope scope,
			int order, InsnList asmCode) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.asmCode = asmCode;
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

	@Override
	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}

}
