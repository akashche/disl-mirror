package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.PrintStream;

import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime.MyWeakKeyIdentityHashMap.EntryDumper;

public class TabSeparatedValuesDumper implements EntryDumper<MyWeakReference<Object>, FieldState[]> {
	private final PrintStream ps;

	public TabSeparatedValuesDumper(PrintStream ps) {
		this.ps = ps;
		
		dumpHeader();
	}

	private void dumpHeader() {
		ps.print("Object ID");
		ps.print('\t');
		ps.print("Field ID");
		ps.print('\t');
		ps.print("Field State");
		ps.print('\t');
		ps.print("Default Init?");
		ps.print('\t');
		ps.print("# Read Acesses");
		ps.print('\t');
		ps.print("# Write Acesses");
		ps.print('\n');
	}

	@Override
	public void dumpEntry(MyWeakReference<Object> key, FieldState[] value) {
		if (value != null) {
			for (int i = 0; i < value.length; i++){
				ps.print(key.objectID);
				ps.print('\t');
				if (value[i] != null) {
					ps.print(value[i].getFieldId());
					ps.print('\t');
					ps.print(value[i].getState());
					ps.print('\t');
					ps.print(value[i].isDefaultInit());
					ps.print('\t');
					ps.print(value[i].getNumReads());
					ps.print('\t');
					ps.print(value[i].getNumWrites());
				}
				ps.print('\n');
				
				//TODO: do something for unaccessed fields!
			}
		}

		ps.print('\n');
		ps.flush();
	}

	@Override
	public void close() {
		ps.close();
	}
}
