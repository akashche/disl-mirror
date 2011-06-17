package ch.usi.dag.disl.snippet.scope;

import org.objectweb.asm.tree.MethodNode;

public interface Scope {

	boolean matches(MethodNode method);
}
