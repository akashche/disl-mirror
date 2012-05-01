package ch.usi.dag.disl.staticcontext;

public abstract class ClassAnalysisContext<V> extends
		AnalysisContext<String, V> {

	@Override
	protected final String key() {
		return staticContextData.getClassNode().name;
	}
}
