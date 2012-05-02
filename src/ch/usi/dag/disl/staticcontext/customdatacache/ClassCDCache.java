package ch.usi.dag.disl.staticcontext.customdatacache;

public abstract class ClassCDCache<V> extends CustomDataCache<String, V> {

	@Override
	protected final String key() {
		return staticContextData.getClassNode().name;
	}
}
