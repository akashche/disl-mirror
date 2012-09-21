package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

/**
 * Class information for arrays
 */
public class ArrayClassInfo extends AbstractClassInfo {

	private int arrayDimensions;
	private ClassInfo arrayComponentInfo;
	
	private static final FieldInfo[] NO_FIELDS = new FieldInfo[0];
	
	public ArrayClassInfo(int classId, String classSignature,
			String classGenericStr, NetReference classLoaderNR,
			ClassInfo superClassInfo, int arrayDimensions,
			ClassInfo arrayComponentInfo) {
		
		super(classId, classSignature, classGenericStr, classLoaderNR,
				superClassInfo);

		this.arrayDimensions = arrayDimensions;
		this.arrayComponentInfo = arrayComponentInfo;
	}

	public ClassNode getClassNode() {
		return null;
	}
	
	public boolean isArray() {
		return true;
	}

	public int getArrayDimensions() {
		return arrayDimensions;
	}

	public ClassInfo getComponentType() {
		return arrayComponentInfo;
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
		return false;
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
		String componentName = arrayComponentInfo.getName();
		StringBuffer buffer = new StringBuffer(arrayDimensions + componentName.length());
		
		for (int i = 0; i < arrayDimensions; i++)
		buffer.append('[');
			buffer.append(componentName);
		
		return buffer.toString();
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
		return NO_FIELDS;
	}

	public FieldInfo getField(String fieldName) throws NoSuchFieldException {
		throw new NoSuchFieldException("Arrays do not have fields");
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
		return NO_FIELDS;
	}

	public FieldInfo getDeclaredField(String fieldName)
			throws NoSuchFieldException {
		throw new NoSuchFieldException("Arrays do not have fields");
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
