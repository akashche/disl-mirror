package ch.usi.dag.disl.example.sharing.instrument;

import static org.objectweb.asm.Type.INT_TYPE;

import org.objectweb.asm.tree.FieldInsnNode;

import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class FieldAccessStaticContext extends MethodStaticContext {

	/**
	 * The character used to separate the components of the allocation site identifier.
	 */
	public static final char SUBSEP = '\034';

	public String getOwner() {
		return ((FieldInsnNode) staticContextData.getRegionStart()).owner;
	}

	public String getFieldId() {
		return getName() + SUBSEP + getDesc();
	}

	public Object getArrayLengthPseudoFieldId() {
		return "$arraylength" + SUBSEP + INT_TYPE.getDescriptor();
	}

	private String getName() {
		return ((FieldInsnNode) staticContextData.getRegionStart()).name;
	}

	private String getDesc() {
		return ((FieldInsnNode) staticContextData.getRegionStart()).desc;
	}
}
