package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;

public class FieldStateList  {
	private final LinkedList<FieldState> myLL;

	public FieldStateList() {
		myLL = new LinkedList<FieldState>();
	}


	public FieldState put(String fieldName) {
		FieldState newFS;
		myLL.add(newFS = new FieldState(fieldName));
		return newFS;
	}

	public FieldState get(String fieldName) {
		if (myLL.size() == 0)
			return null;
		Iterator<FieldState> it = myLL.iterator();
		while(it.hasNext()) {
			FieldState nextFS;
			if ((nextFS = it.next()).getFieldName().equals(fieldName)){
				return nextFS;
			}
		}
		return null;
	}

	public void dump(PrintStream ps) {
		for(FieldState fs : myLL) {
			fs.dump(ps);
		}
	}
}
