package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.MyWeakKeyIdentityHashMap.EntryDumper;



public class MyDumper implements EntryDumper<MyWeakReference<Object>, ConcurrentHashMap<String, FieldState>>{
	
	private static final String OFFSET = "\t\t";
	private static final String SEPARATOR = "\t";

	private final PrintStream ps;

	public MyDumper(PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public synchronized void dumpEntry(MyWeakReference<Object> key, ConcurrentHashMap<String, FieldState> value) {
		String shortId = (String) key.objectID.subSequence(key.objectID.indexOf(":")+1,  key.objectID.length());
		ps.println(shortId);
		for(Entry<String, FieldState> entry : value.entrySet()) {
			ps.println(OFFSET + entry.getKey() + SEPARATOR + entry.getValue().toString());
		}
		ps.flush();
	}

	@Override
	public void close() {
		ps.flush();
		ps.close();

	}
}
