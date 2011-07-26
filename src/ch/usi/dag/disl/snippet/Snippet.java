package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

public class Snippet implements Comparable<Snippet> {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected InsnList asmCode;
	protected Set<SyntheticLocalVar> syntheticLocalVars;
	protected Map<String, Method> staticAnalyses;
	protected boolean usesDynamicAnalysis;
	
	public Snippet(Class<?> annotationClass,
			Marker marker,
			Scope scope,
			int order, InsnList asmCode,
			Set<SyntheticLocalVar> syntheticLocalVars,
			Map<String, Method> staticAnalyses,
			boolean usesDynamicAnalysis) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.asmCode = asmCode;
		this.syntheticLocalVars = syntheticLocalVars;
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
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
	
	public Set<SyntheticLocalVar> getSyntheticLocalVars() {
		return syntheticLocalVars;
	}

	public Map<String, Method> getStaticAnalyses() {
		return staticAnalyses;
	}
	
	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}

	public void prepare(boolean useDynamicBypass) {
		
		// remove returns in snippet (in asm code)
		InsnListHelper.removeReturns(asmCode);
		
		if(! useDynamicBypass) {
			return;
		}
		// TODO ! dynamic bypass
	}
	
	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}
}
