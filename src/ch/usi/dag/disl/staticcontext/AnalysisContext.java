package ch.usi.dag.disl.staticcontext;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public abstract class AnalysisContext<K, V> extends AbstractStaticContext {

	private Map<K, V> analysisCache = new HashMap<K, V>();
	protected V thisAnalysis;

	public void staticContextData(Shadow sa) {

		staticContextData = sa;
		thisAnalysis = analysisCache.get(key());

		if (thisAnalysis == null) {

			thisAnalysis = analysis();
			analysisCache.put(key(), thisAnalysis);
		}
	}

	protected abstract K key();
	protected abstract V analysis();

}
