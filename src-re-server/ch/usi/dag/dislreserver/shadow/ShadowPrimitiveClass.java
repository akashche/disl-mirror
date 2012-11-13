package ch.usi.dag.dislreserver.shadow;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class ShadowPrimitiveClass extends ShadowClass {

	private Type t;

	ShadowPrimitiveClass(long net_ref, ShadowObject classLoader, Type t) {
		super(net_ref, classLoader);

		this.t = t;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public ShadowClass getComponentType() {
		return null;
	}

	@Override
	public boolean isInstance(ShadowObject obj) {
		return false;
	}

	@Override
	public boolean isAssignableFrom(ShadowClass klass) {
		return equals(klass);
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public boolean isSynthetic() {
		return false;
	}

	@Override
	public boolean isEnum() {
		return false;
	}

	@Override
	public String getName() {
		switch (t.getSort()) {
		case Type.BOOLEAN:
			return "boolean";
		case Type.BYTE:
			return "byte";
		case Type.CHAR:
			return "char";
		case Type.DOUBLE:
			return "double";
		case Type.FLOAT:
			return "float";
		case Type.INT:
			return "int";
		case Type.LONG:
			return "long";
		case Type.SHORT:
			return "short";

		default:
			throw new DiSLREServerFatalException("Unknown primitive type");
		}
	}

	@Override
	public String getCanonicalName() {
		return getName();
	}

	@Override
	public String[] getInterfaces() {
		return new String[0];
	}

	@Override
	public String getPackage() {
		return null;
	}

	@Override
	public ShadowClass getSuperclass() {
		return null;
	}

}
