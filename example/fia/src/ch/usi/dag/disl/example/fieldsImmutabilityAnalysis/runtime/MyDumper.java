package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicLongArray;

import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.MyWeakKeyIdentityHashMap.EntryDumper;

public class MyDumper implements EntryDumper<MyWeakReference<Object>, AtomicLongArray>{
	private final PrintStream ps;

	public MyDumper(PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public void dumpEntry(MyWeakReference<Object> key, AtomicLongArray value) {
		ps.println(key.objectID);
		StringBuilder str = new StringBuilder("[");
		for(int i = 0; i < value.length(); i++){
			str.append((value.get(i)));
			str.append(",");
		}
		if(str.indexOf(",")>0)
			str.deleteCharAt(str.lastIndexOf(","));
		str.append("]");
		ps.println(str);
		ps.flush();
	}

	@Override
	public void close() {
		ps.flush();
		ps.close();
	}
}
