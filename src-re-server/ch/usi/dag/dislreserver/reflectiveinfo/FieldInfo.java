package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.FieldNode;

public interface FieldInfo {

	public FieldNode getFieldNode();
	
	public String getName();

	public int getModifiers();

	public String getType();

	public boolean isPublic();
}
