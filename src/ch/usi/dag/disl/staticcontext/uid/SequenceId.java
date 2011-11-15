package ch.usi.dag.disl.staticcontext.uid;

public class SequenceId extends AbstractIdCalculator {

	private int nextId = 0;

	// get sequential id
	protected int getId() {
		
		int newId = nextId;
		++nextId;
		return newId;
	}
	
	
}
