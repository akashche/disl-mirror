package ch.usi.dag.disl.example.sharing.runtime;

import java.io.PrintStream;

import ch.usi.dag.disl.example.sharing.runtime.MyWeakKeyIdentityHashMap.EntryDumper;

public class MyDumper implements EntryDumper<MyWeakReference<Object>, FieldState[]>{
	private final PrintStream ps;

	public MyDumper(PrintStream ps) {
		this.ps = ps;
	}

	@Override
	public void dumpEntry(MyWeakReference<Object> key, FieldState[] value) {
		ps.println(key.objectID);
		if(value != null) {
			for(int i = 0; i < value.length; i++){
				if (value[i] != null){
					ps.println(value[i].toString());
				}
				//TODO: do something for unaccessed fields!
			}
		}
		ps.println("");
		ps.flush();
	}

	@Override
	public void close() {
		ps.flush();
		ps.close();
	}
}
