package ch.usi.dag.disl.staticinfo.analysis.uid;

import ch.usi.dag.disl.staticinfo.analysis.AbstractStaticAnalysis;

public abstract class AbstractUniqueId extends AbstractStaticAnalysis {

	private IdHolder idHolder;
	
	// constructor for static analysis
	protected AbstractUniqueId() {
		
		idHolder = null;
	}
	
	// constructor for singleton
	protected AbstractUniqueId(AbstractIdCalculator idCalc,
			String outputFileName) {
		
		idHolder = new IdHolder(idCalc, outputFileName);
	}
	
	public int get() {

		return getSingleton().idHolder.getID(idFor());
	}
	
	// String value for which the id will be computed 
	protected abstract String idFor();

	// Singleton that holds the data
	protected abstract AbstractUniqueId getSingleton();
}
