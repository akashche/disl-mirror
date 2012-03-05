package ch.usi.dag.disl.example.sync.runtime;

import java.io.PrintStream;

/**
 * An object on the shadow heap. It <em>must</em> not keep a strong reference to the object it is shadowing.
 * 
 * @author Andreas Sewe
 */
public interface ShadowObject {

	void dump(PrintStream out);
}
