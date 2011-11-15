package ch.usi.dag.disl.staticcontext.uid;

import java.util.Collection;
import java.util.Random;

public class RandomId extends AbstractIdCalculator {

	private Random random;
	
	public RandomId() {
		
		random = new Random();
	}
	
	public RandomId(long seed) {
		
		random = new Random(seed);
	}
	
	protected int getId() {

		Collection<Integer> allreadyAssigned = strToId.values();
		
		// generate unique random id
		int newId = random.nextInt(Integer.MAX_VALUE); // only positive values
		
		while(allreadyAssigned.contains(newId)) {
			newId = random.nextInt(Integer.MAX_VALUE); // only positive values
		}
		
		return newId;
	}

}
