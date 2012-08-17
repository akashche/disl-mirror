package ch.usi.dag.disl.localvar;

import java.util.HashMap;
import java.util.Map;

public class LocalVars {

	private Map<String, SyntheticLocalVar> syntheticLocals = 
		new HashMap<String, SyntheticLocalVar>();
	private Map<String, ThreadLocalVar> threadLocals = 
		new HashMap<String, ThreadLocalVar>();
	private Map<String, SyntheticStaticFieldVar> syntheticStaticFields = 
			new HashMap<String, SyntheticStaticFieldVar>();

	public Map<String, SyntheticLocalVar> getSyntheticLocals() {
		return syntheticLocals;
	}

	public Map<String, ThreadLocalVar> getThreadLocals() {
		return threadLocals;
	}

	public Map<String, SyntheticStaticFieldVar> getSyntheticStaticFields() {
		return syntheticStaticFields;
	}
	
	public void putAll(LocalVars localVars) {
		
		syntheticLocals.putAll(localVars.getSyntheticLocals());
		threadLocals.putAll(localVars.getThreadLocals());
		syntheticStaticFields.putAll(localVars.getSyntheticStaticFields());
	}
}
