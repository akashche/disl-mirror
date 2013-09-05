package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.Map;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.processor.Proc;
import ch.usi.dag.disl.scope.Scope;

public class Snippet implements Comparable<Snippet> {

	private String originClassName;
	private String originMethodName;
	
	private Class<?> annotationClass;
	private Marker marker;
	private Scope scope;
	private Method guard;
	private int order;
	private SnippetUnprocessedCode unprocessedCode;
	private SnippetCode code;

	public Snippet(String originClassName, String originMethodName,
			Class<?> annotationClass, Marker marker, Scope scope, Method guard,
			int order, SnippetUnprocessedCode unprocessedCode) {
		super();
		this.originClassName = originClassName;
		this.originMethodName = originMethodName;
		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.guard = guard;
		this.order = order;
		this.unprocessedCode = unprocessedCode;
	}

	public String getOriginClassName() {
		return originClassName;
	}

	public String getOriginMethodName() {
		return originMethodName;
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

	public Method getGuard() {
		return guard;
	}
	
	public int getOrder() {
		return order;
	}

	public SnippetCode getCode() {
		return code;
	}

	public int compareTo(Snippet o) {
		return o.getOrder() - order;
	}

	public void init(LocalVars allLVs, Map<Type, Proc> processors,
			boolean exceptHandler, boolean useDynamicBypass)
			throws StaticContextGenException, ReflectionException,
			ProcessorException {

		code = unprocessedCode.process(allLVs, processors, marker,
				exceptHandler, useDynamicBypass);
		unprocessedCode = null;
	}
}
