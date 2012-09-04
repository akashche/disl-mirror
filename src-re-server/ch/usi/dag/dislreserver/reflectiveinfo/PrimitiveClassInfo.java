package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

/**
 * Class information for primitive types
 */
public class PrimitiveClassInfo extends AbstractClassInfo {

	public PrimitiveClassInfo(int classId, String classSignature,
			String classGenericStr, NetReference classLoaderNR,
			ClassInfo superClassInfo) {
		super(classId, classSignature, classGenericStr, classLoaderNR,
				superClassInfo);
	}

	public ClassNode getClassNode() {
		return null;
	}
	
	public boolean isArray() {
		return false;
	}

	public int getArrayDimensions() {
		return -1;
	}

	public ClassInfo getComponentType() {
		return null;
	}

	public boolean isInstance(NetReference nr) {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public boolean isAssignableFrom(ClassInfo ci) {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public boolean isInterface() {
		return false;
	}

	public boolean isPrimitive() {
		return true;
	}

	public boolean isAnnotation() {
		return false;
	}

	public boolean isSynthetic() {
		return false;
	}

	public boolean isEnum() {
		return false;
	}

	public String getName() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String getCanonicalName() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String[] getInterfaces() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String getPackage() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public FieldInfo[] getFields() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public FieldInfo getField(String fieldName) throws NoSuchFieldException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo[] getMethods() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo getMethod(String methodName, ClassInfo[] argumentCIs)
			throws NoSuchMethodException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo getMethod(String methodName, String[] argumentNames)
			throws NoSuchMethodException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String[] getDeclaredClasses() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public FieldInfo[] getDeclaredFields() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public FieldInfo getDeclaredField(String fieldName)
			throws NoSuchFieldException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo[] getDeclaredMethods() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo getDeclaredMethod(String methodName,
			ClassInfo[] argumentCIs) throws NoSuchMethodException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public MethodInfo getDeclaredMethod(String methodName,
			String[] argumentNames) throws NoSuchMethodException {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}
}
