package ch.usi.dag.disl.scope;

import org.objectweb.asm.tree.MethodNode;

public interface Scope {

	public boolean matches(String className, MethodNode method);
}