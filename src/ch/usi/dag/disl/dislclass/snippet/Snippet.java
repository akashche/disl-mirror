package ch.usi.dag.disl.dislclass.snippet;

import java.util.Map;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.snippet.marker.Marker;
import ch.usi.dag.disl.dislclass.snippet.scope.Scope;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.guard.SnippetGuard;

public class Snippet implements Comparable<Snippet> {

	private Class<?> annotationClass;
	private Marker marker;
	private Scope scope;
	private SnippetGuard guard;
	private int order;
	private SnippetUnprocessedCode unprocessedCode;
	private SnippetCode code;

	public Snippet(Class<?> annotationClass, Marker marker, Scope scope,
			SnippetGuard guard, int order, SnippetUnprocessedCode unprocessedCode) {
		super();
		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.guard = guard;
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

	public SnippetGuard getGuard() {
		return guard;
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

	public void init(LocalVars allLVs, Map<Type, Proc> processors,
			boolean allDynamicBypass) throws StaticInfoException,
			ReflectionException, ProcessorException {

		code = unprocessedCode.process(allLVs, processors, marker,
				allDynamicBypass);
		unprocessedCode = null;
	}
}
