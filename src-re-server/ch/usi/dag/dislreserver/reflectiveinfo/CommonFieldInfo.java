package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class CommonFieldInfo implements FieldInfo {

	private FieldNode fieldNode;
	private int modifiers;
	private String name;
	private String type;

	public CommonFieldInfo(FieldNode fieldNode) {
		
		this.fieldNode = fieldNode;
		name = fieldNode.name;
		type = fieldNode.desc;
		modifiers = fieldNode.access;
	}

	public FieldNode getFieldNode() {
		return fieldNode;
	}
	
	public String getName() {
		return name;
	}

	public int getModifiers() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String getType() {
		return type;
	}

	public boolean isPublic() {
		return (modifiers & Opcodes.ACC_PUBLIC) != 0;
	}
}
