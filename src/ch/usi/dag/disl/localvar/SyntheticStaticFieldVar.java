package ch.usi.dag.disl.localvar;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.annotation.SyntheticStaticField.Scope;

public class SyntheticStaticFieldVar extends AbstractLocalVar {

	private int access;
	private Scope scope;

	public SyntheticStaticFieldVar(String className, String fieldName, Type type,
			Scope scope, int access) {

		super(className, fieldName, type);
		this.scope = scope;
		this.access = access;
	}

	public String getTypeAsDesc() {
		return getType().getDescriptor();
	}

	public int getAccess() {
		return access;
	}

	public Scope getScope() {
		return scope;
	}

}
