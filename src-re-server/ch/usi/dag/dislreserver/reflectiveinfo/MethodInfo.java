package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.MethodNode;

public interface MethodInfo {

	public MethodNode getMethodNode();
	
	public String getName();

	public int getModifiers();

	public String getReturnType();

	public String[] getParameterTypes();

	public String[] getExceptionTypes();

	public boolean isPublic();
}
