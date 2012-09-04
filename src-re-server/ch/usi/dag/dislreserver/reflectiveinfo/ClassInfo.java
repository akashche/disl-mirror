package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.dislreserver.netreference.NetReference;

public interface ClassInfo {

	public ClassNode getClassNode();
	
	public int getId();

	public String getSignature();

	public String getGenericStr();

	public boolean isArray();

	public int getArrayDimensions();

	public ClassInfo getComponentType();

	public NetReference getClassLoaderNR();

	public boolean isInstance(NetReference nr);

	public boolean isAssignableFrom(ClassInfo ci);

	public boolean isInterface();

	public boolean isPrimitive();

	public boolean isAnnotation();

	public boolean isSynthetic();

	public boolean isEnum();

	public String getName();

	public String getCanonicalName();

	public String[] getInterfaces();
	
	public String getPackage();
	
	public ClassInfo getSuperclass();

	public FieldInfo[] getFields();

	public FieldInfo getField(String fieldName) throws NoSuchFieldException;
	
	public MethodInfo[] getMethods();
	
	public MethodInfo getMethod(String methodName, ClassInfo[] argumentCIs)
			throws NoSuchMethodException;

	public MethodInfo getMethod(String methodName, String[] argumentNames)
			throws NoSuchMethodException;
	
	public String[] getDeclaredClasses();

	public FieldInfo[] getDeclaredFields();

	public FieldInfo getDeclaredField(String fieldName)
			throws NoSuchFieldException;
	
	public MethodInfo[] getDeclaredMethods();

	public MethodInfo getDeclaredMethod(String methodName,
			ClassInfo[] argumentCIs) throws NoSuchMethodException;

	public MethodInfo getDeclaredMethod(String methodName,
			String[] argumentNames) throws NoSuchMethodException;
}
