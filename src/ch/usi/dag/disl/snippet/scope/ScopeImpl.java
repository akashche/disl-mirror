package ch.usi.dag.disl.snippet.scope;

import org.objectweb.asm.tree.MethodNode;

public class ScopeImpl implements Scope {

	public ScopeImpl(String scopeExpression) {
		// TODO implement
	}

	public boolean matches(String className, MethodNode method) {
		
		// TODO implement
		
		return className.equals("TargetClass");
	}

}
