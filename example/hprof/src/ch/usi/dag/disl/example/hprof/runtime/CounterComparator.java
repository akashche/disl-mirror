package ch.usi.dag.disl.example.hprof.runtime;

import java.util.Comparator;

public class CounterComparator implements Comparator<Counter> {

    @Override
    public int compare(Counter arg0, Counter arg1) {
        return (arg0.getTotalSize() > arg1.getTotalSize()) ? 1 : -1;
    }
}
