package ch.usi.dag.disl.staticcontext.uid;

import java.util.Map;

public abstract class AbstractIdCalculator {

	Map<String, Integer> strToId;

	void regIdMap(Map<String, Integer> strToId) {
		
		this.strToId = strToId;
	}
	
	protected abstract int getId();
}