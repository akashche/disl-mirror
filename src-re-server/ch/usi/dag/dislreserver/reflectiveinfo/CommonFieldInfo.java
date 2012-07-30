package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;

public class CommonFieldInfo implements FieldInfo {

	private int modifiers;
	private String name;
	private String type;

	public CommonFieldInfo(FieldNode fieldNode) {
		
		name = fieldNode.name;
		type = fieldNode.desc;
		modifiers = fieldNode.access;
	}

	public String getName() {
		return name;
	}

	public int getModifiers() {
		return modifiers;
	}

	public String getType() {
		return type;
	}

	public boolean isPublic() {
		return (modifiers & Opcodes.ACC_PUBLIC) != 0;
	}
}
