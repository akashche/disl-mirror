package ch.usi.dag.disl.snippet.localvars;

import java.util.HashMap;
import java.util.Map;

public class LocalVars {

	Map<String, SyntheticLocalVar> syntheticLocals = 
		new HashMap<String, SyntheticLocalVar>();
	Map<String, ThreadLocalVar> threadLocals = 
		new HashMap<String, ThreadLocalVar>();

	public Map<String, SyntheticLocalVar> getSyntheticLocals() {
		return syntheticLocals;
	}

	public Map<String, ThreadLocalVar> getThreadLocals() {
		return threadLocals;
	}
	
	public void putAll(LocalVars localVars) {
		
		syntheticLocals.putAll(localVars.getSyntheticLocals());
		threadLocals.putAll(localVars.getThreadLocals());
	}
}
