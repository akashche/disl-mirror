package ch.usi.dag.disl.example.hprof.runtime;

import java.util.Comparator;



public class CounterComparator implements Comparator<Object> {

	@Override
	public int compare(Object arg0, Object arg1) {
		long arg0Size = ((Counter) arg0).getTotalSize();
		long arg1Size = ((Counter) arg1).getTotalSize();

		if(arg0Size > arg1Size)
			return -1;
		else if (arg0Size < arg1Size)
			return 1;
		else
			return 0;
	}
}

