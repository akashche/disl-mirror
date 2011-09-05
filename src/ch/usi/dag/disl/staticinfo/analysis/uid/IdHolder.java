package ch.usi.dag.disl.staticinfo.analysis.uid;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.StaticAnalysisException;

// holds IDs
class IdHolder {

	private PrintWriter output = null;

	private AbstractIdCalculator idCalc;
	
	private Map<String, Integer> strToId = new HashMap<String, Integer>();
	
	public IdHolder(AbstractIdCalculator idCalc, String outputFileName) {

		// register id calculator
		this.idCalc = idCalc;
		idCalc.regIdMap(strToId);
		
		// create output
		try {
			output = new PrintWriter(outputFileName);
		} catch (FileNotFoundException e) {
			throw new StaticAnalysisException(
					"Cannot create output for AbstractUniqueId analysis", e);
		}
		
		// register shutdown hook - output close
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				output.close();
			}
		});
	}

	public synchronized int getID(String forValue) {
		
		Integer alreadyAssignedId = strToId.get(forValue);
		
		if(alreadyAssignedId != null) {
			return alreadyAssignedId;
		}
		
		return newID(forValue);
	}
	
	private int newID(String forValue) {
		
		int newId = idCalc.getId();

		// put into map
		strToId.put(forValue, newId);
		
		// dump to the file
		output.println(newId + "\t" + forValue);
		
		return newId;
	}
}
