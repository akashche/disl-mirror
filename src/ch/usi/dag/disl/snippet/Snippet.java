package ch.usi.dag.disl.snippet;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.snippet.localvars.LocalVars;
import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;

public class Snippet implements Comparable<Snippet> {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected UnprocessedSnippetCode unprocessedCode;
	protected SnippetCode code;

	public Snippet(Class<?> annotationClass, Marker marker, Scope scope,
			int order, UnprocessedSnippetCode unprocessedCode) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.unprocessedCode = unprocessedCode;
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

	public SnippetCode getCode() {
		return code;
	}

	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}

	public void prepare(LocalVars allLVs, boolean useDynamicBypass)
			throws StaticAnalysisException, ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;

		// prepare code
		code.prepare(useDynamicBypass);
	}
}
