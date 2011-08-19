package ch.usi.dag.disl.dislclass.snippet;

import java.util.Map;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.processor.Processor;
import ch.usi.dag.disl.dislclass.snippet.marker.Marker;
import ch.usi.dag.disl.dislclass.snippet.scope.Scope;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class Snippet implements Comparable<Snippet> {

	private Class<?> annotationClass;
	private Marker marker;
	private Scope scope;
	private int order;
	private SnippetUnprocessedCode unprocessedCode;
	private SnippetCode code;

	public Snippet(Class<?> annotationClass, Marker marker, Scope scope,
			int order, SnippetUnprocessedCode unprocessedCode) {
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

	public void init(LocalVars allLVs, Map<Class<?>, Processor> processors,
			boolean useDynamicBypass) throws StaticAnalysisException,
			ReflectionException, ProcessorException {

		code = unprocessedCode.process(allLVs, processors, useDynamicBypass);
		unprocessedCode = null;
	}
}
