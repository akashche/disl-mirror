package ch.usi.dag.disl.scope;

import org.objectweb.asm.tree.MethodNode;

public interface Scope {

	boolean matches(MethodNode method);
}
