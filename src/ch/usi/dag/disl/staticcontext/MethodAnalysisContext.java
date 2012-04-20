package ch.usi.dag.disl.staticcontext;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public abstract class MethodAnalysisContext<V> extends AbstractStaticContext {

	private Map<MethodNode, V> analysisCache = new HashMap<MethodNode, V>();
	protected V thisAnalysis;

	public void staticContextData(Shadow sa) {

		staticContextData = sa;
		MethodNode method = sa.getMethodNode();
		thisAnalysis = analysisCache.get(method);

		if (thisAnalysis == null) {

			thisAnalysis = analysis(method);
			analysisCache.put(method, thisAnalysis);
		}
	}

	public abstract V analysis(MethodNode method);

}
