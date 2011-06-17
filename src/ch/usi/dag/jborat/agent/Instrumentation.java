package ch.usi.dag.jborat.agent;

import org.objectweb.asm.tree.ClassNode;

// TODO this interface should be replaced by jborat jar

public interface Instrumentation {

	public void instrument(ClassNode clazz );
}
