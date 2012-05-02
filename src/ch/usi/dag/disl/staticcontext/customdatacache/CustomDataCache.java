package ch.usi.dag.disl.staticcontext.customdatacache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public abstract class CustomDataCache<K, V> extends AbstractStaticContext {

	private Map<K, V> cache = new HashMap<K, V>();
	protected V customData;

	public void staticContextData(Shadow sa) {

		super.staticContextData(sa);
		
		customData = cache.get(key());

		if (customData == null) {

			customData = produceCustomData();
			cache.put(key(), customData);
		}
	}

	protected abstract K key();
	protected abstract V produceCustomData();

}
