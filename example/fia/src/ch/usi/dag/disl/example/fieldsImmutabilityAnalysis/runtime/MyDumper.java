package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;

import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.MyWeakKeyIdentityHashMap.EntryDumper;



public class MyDumper implements EntryDumper<MyWeakReference<Object>, FieldStateList>{
	private final PrintStream ps;

	public MyDumper(PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public synchronized void dumpEntry(MyWeakReference<Object> key, FieldStateList value) {
		String shortId = (String) key.objectID.subSequence(key.objectID.indexOf(":")+1,  key.objectID.length());
		ps.println(shortId);
		value.dump(ps);
		ps.flush();
	}

	@Override
	public void close() {
		ps.flush();
		ps.close();
	}
}
