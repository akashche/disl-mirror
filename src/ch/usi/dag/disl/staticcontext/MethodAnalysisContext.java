package ch.usi.dag.disl.staticcontext;

public abstract class MethodAnalysisContext<V> extends
		AnalysisContext<String, V> {

	@Override
	protected final String key() {
		return staticContextData.getClassNode().name
				+ staticContextData.getMethodNode().name
				+ staticContextData.getMethodNode().desc;
	}
}
