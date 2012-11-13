package ch.usi.dag.dislreserver.shadow;

import java.util.Arrays;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class ShadowArrayClass extends ShadowClass {

	private Type t;
	private ShadowClass superClass;
	private ShadowClass arrayComponentClass;

	ShadowArrayClass(long net_ref, ShadowObject classLoader,
			ShadowClass superClass, 
			ShadowClass arrayComponentClass, Type t) {
		super(net_ref, classLoader);

		this.t = t;
		this.superClass = superClass;
		this.arrayComponentClass = arrayComponentClass;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	public int getArrayDimensions() {
		return t.getDimensions();
	}

	@Override
	public ShadowClass getComponentType() {
		// return arrayComponentClass;
		throw new DiSLREServerFatalException(
				"ShadowArrayClass.getComponentType not implemented");
	}

	@Override
	public boolean isInstance(ShadowObject obj) {
		return equals(obj.getSClass());
	}

	@Override
	public boolean isAssignableFrom(ShadowClass klass) {
		return equals(klass)
				|| ((klass instanceof ShadowArrayClass) && arrayComponentClass
						.isAssignableFrom(klass.getComponentType()));
	}

	@Override
	public boolean isInterface() {
		return false;
	}

	@Override
	public boolean isPrimitive() {
		return false;
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
		return t.getDescriptor().replace('/', '.');
	}

	@Override
	public String getCanonicalName() {
		return t.getClassName();
	}

	@Override
	public String[] getInterfaces() {
		return new String[] { "java.lang.Cloneable", "java.io.Serializable" };
	}

	@Override
	public String getPackage() {
		return null;
	}

	@Override
	public ShadowClass getSuperclass() {
		return superClass;
	}

	@Override
	public FieldInfo[] getFields() {
		return new FieldInfo[0];
	}

	@Override
	public FieldInfo getField(String fieldName) throws NoSuchFieldException {
		throw new NoSuchFieldException(t.getClassName() + "." + fieldName);
	}

	@Override
	public MethodInfo[] getMethods() {
		return getSuperclass().getMethods();
	}

	@Override
	public MethodInfo getMethod(String methodName, String[] argumentNames)
			throws NoSuchMethodException {

		for (MethodInfo methodInfo : superClass.getMethods()) {
			if (methodName.equals(methodInfo.getName())
					&& Arrays.equals(argumentNames,
							methodInfo.getParameterTypes())) {
				return methodInfo;
			}
		}

		throw new NoSuchMethodException(t.getClassName() + "." + methodName
				+ argumentNamesToString(argumentNames));
	}

	@Override
	public String[] getDeclaredClasses() {
		return new String[0];
	}

	@Override
	public FieldInfo[] getDeclaredFields() {
		return new FieldInfo[0];
	}

	@Override
	public FieldInfo getDeclaredField(String fieldName)
			throws NoSuchFieldException {
		throw new NoSuchFieldException(t.getClassName() + "." + fieldName);
	}

	@Override
	public MethodInfo[] getDeclaredMethods() {
		return new MethodInfo[0];
	}

	@Override
	public MethodInfo getDeclaredMethod(String methodName,
			String[] argumentNames) throws NoSuchMethodException {
		throw new NoSuchMethodException(t.getClassName() + "." + methodName
				+ argumentNamesToString(argumentNames));
	}

}
