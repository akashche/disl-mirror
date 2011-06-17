package ch.usi.dag.disl;

import org.objectweb.asm.tree.ClassNode;

//FIXME When integrating with jborat, add the implement field
// and the override annotation.  
public class DiSLInstrumentation {
	public void instrument(ClassNode clazz) {
		// Four steps to instrument classes:
		// Parser.parse();		
		// Marker.mark(method);
		// Analyzer.analyse(clazz);
		// Weaver.weave(clazz);
	}
}
