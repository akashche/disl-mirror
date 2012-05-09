package ch.usi.dag.disl.example.racer;

import org.objectweb.asm.tree.FieldInsnNode;

import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class FieldAccessStaticContext extends MethodStaticContext {



	public String getOwner() {
		return ((FieldInsnNode) staticContextData.getRegionStart()).owner;
	}

	public String getFieldId() {
		FieldInsnNode fin = ((FieldInsnNode) staticContextData.getRegionStart());
		return fin.owner + "." + fin.name;
	}

}
