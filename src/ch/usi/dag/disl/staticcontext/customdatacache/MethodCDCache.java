package ch.usi.dag.disl.staticcontext.customdatacache;

public abstract class MethodCDCache<V> extends CustomDataCache<String, V> {

	@Override
	protected final String key() {
		return staticContextData.getClassNode().name
				+ staticContextData.getMethodNode().name
				+ staticContextData.getMethodNode().desc;
	}
}
