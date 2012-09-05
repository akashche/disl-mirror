package ch.usi.dag.dislreserver.reflectiveinfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

/**
 * Class information for common classes
 */
public class CommonClassInfo extends AbstractClassInfo {

	private ClassNode classNode;
	
	CommonClassInfo(int classId, String classSignature, String classGenericStr,
			NetReference classLoaderNR, ClassInfo superClassInfo,
			byte[] classCode) {
		
		super(classId, classSignature, classGenericStr, classLoaderNR,
				superClassInfo);

		if (classCode == null || classCode.length == 0) {
			throw new DiSLREServerFatalException(
					"Creating class info for " + classSignature
					+ " with no code provided");
		}
		
		initializeClassInfo(classCode);
	}
	
	public ClassNode getClassNode() {
		return classNode;
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
	
	private void initializeClassInfo(byte[] classCode) {

		ClassReader classReader = new ClassReader(classCode);
		classNode = new ClassNode(Opcodes.ASM4);
		classReader.accept(classNode, ClassReader.SKIP_DEBUG
				| ClassReader.EXPAND_FRAMES);

		access = classNode.access;
		name = classNode.name.replace('/', '.');

		List<MethodInfo> methodsLst = new ArrayList<MethodInfo>(classNode.methods.size());
		List<MethodInfo> publicMethodsLst = new LinkedList<MethodInfo>();

		for (MethodNode methodNode : classNode.methods) {

			MethodInfo methodInfo = new CommonMethodInfo(methodNode);
			methodsLst.add(methodInfo);

			if (methodInfo.isPublic()) {
				publicMethodsLst.add(methodInfo);
			}
		}

		List<FieldInfo> fieldsLst = new ArrayList<FieldInfo>(classNode.fields.size());
		List<FieldInfo> publicFieldsLst = new LinkedList<FieldInfo>();

		for (FieldNode fieldNode : classNode.fields) {

			FieldInfo fieldInfo = new CommonFieldInfo(fieldNode);
			fieldsLst.add(fieldInfo);

			if (fieldInfo.isPublic()) {
				publicFieldsLst.add(fieldInfo);
			}
		}

		if (getSuperclass() != null) {

			for (MethodInfo methodInfo : getSuperclass().getMethods()) {
				publicMethodsLst.add(methodInfo);
			}

			for (FieldInfo fieldInfo : getSuperclass().getFields()) {
				publicFieldsLst.add(fieldInfo);
			}
		}

		List<String> innerClassesLst = new ArrayList<String>(classNode.innerClasses.size());

		for (InnerClassNode innerClassNode : classNode.innerClasses) {
			innerClassesLst.add(innerClassNode.name);
		}
		
		// to have "checked" array :(
		methods = methodsLst.toArray(new MethodInfo[0]);
		publicMethods = publicMethodsLst.toArray(new MethodInfo[0]);
		fields = fieldsLst.toArray(new FieldInfo[0]);
		publicFields = publicFieldsLst.toArray(new FieldInfo[0]);
		interfaces = classNode.interfaces.toArray(new String[0]);
		innerClasses = innerClassesLst.toArray(new String[0]);
	}

	private int access;
	private String name;

	private MethodInfo[] methods;
	private MethodInfo[] publicMethods;
	private FieldInfo[] fields;
	private FieldInfo[] publicFields;
	private String[] interfaces;
	private String[] innerClasses;

	// All these methods should return the value of fields initialized by
	// generateClassInfo(byte[])
	// NOTE: returned arrays should be copies (obtained using
	// Arrays.copyOf(...))
	public boolean isInstance(NetReference nr) {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public boolean isAssignableFrom(ClassInfo ci) {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public boolean isInterface() {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isPrimitive() {
		return false;
	}

	public boolean isAnnotation() {
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}

	public boolean isSynthetic() {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}

	public boolean isEnum() {
		return (access & Opcodes.ACC_ENUM) != 0;
	}

	public String getName() {
		return name;
	}

	public String getCanonicalName() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String[] getInterfaces() {
		
		return Arrays.copyOf(interfaces, interfaces.length);
	}

	public String getPackage() {
		
		int i = name.lastIndexOf('.');

		if (i != -1) {
			return name.substring(0, i);
		} else {
			return null;
		}
	}

	public FieldInfo[] getFields() {
		
		return Arrays.copyOf(publicFields, publicFields.length);
	}

	public FieldInfo getField(String fieldName) throws NoSuchFieldException {

		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.isPublic() && fieldInfo.getName().equals(fieldName)) {
				return fieldInfo;
			}
		}

		if (getSuperclass() == null) {
			throw new NoSuchFieldException(name + "." + fieldName);
		}

		return getSuperclass().getField(fieldName);
	}

	public MethodInfo[] getMethods() {
		
		return Arrays.copyOf(publicMethods, publicMethods.length);
	}

	public MethodInfo getMethod(String methodName, ClassInfo[] argumentCIs)
			throws NoSuchMethodException {
		return getMethod(methodName, classesToStrings(argumentCIs));
	}

	public MethodInfo getMethod(String methodName, String[] argumentNames)
			throws NoSuchMethodException {

		for (MethodInfo methodInfo : methods) {
			if (methodInfo.isPublic()
					&& methodName.equals(methodInfo.getName())
					&& Arrays.equals(argumentNames,
							methodInfo.getParameterTypes())) {
				return methodInfo;
			}
		}

		if (getSuperclass() == null) {
			throw new NoSuchMethodException(name + "." + methodName
					+ argumentNamesToString(argumentNames));
		}

		return getSuperclass().getMethod(methodName, argumentNames);
	}

	public String[] getDeclaredClasses() {
		
		return Arrays.copyOf(innerClasses, innerClasses.length);
	}

	public FieldInfo[] getDeclaredFields() {
		
		return Arrays.copyOf(fields, fields.length);
	}

	public FieldInfo getDeclaredField(String fieldName)
			throws NoSuchFieldException {

		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.getName().equals(fieldName)) {
				return fieldInfo;
			}
		}

		throw new NoSuchFieldException(name + "." + fieldName);
	}

	public MethodInfo[] getDeclaredMethods() {
		
		return Arrays.copyOf(methods, methods.length);
	}

	public MethodInfo getDeclaredMethod(String methodName,
			ClassInfo[] argumentCIs) throws NoSuchMethodException {
		return getDeclaredMethod(methodName, classesToStrings(argumentCIs));
	}

	public MethodInfo getDeclaredMethod(String methodName,
			String[] argumentNames) throws NoSuchMethodException {

		for (MethodInfo methodInfo : methods) {
			if (methodName.equals(methodInfo.getName())
					&& Arrays.equals(argumentNames,
							methodInfo.getParameterTypes())) {
				return methodInfo;
			}
		}

		throw new NoSuchMethodException(name + "." + methodName
				+ argumentNamesToString(argumentNames));
	}

	private static String[] classesToStrings(ClassInfo[] argumentCIs) {

		throw new DiSLREServerFatalException("Not implemented");

		// TODO it should return the same as methodInfo.getParameterTypes()
//		if (argumentCIs == null) {
//			return new String[0];
//		}
//
//		int size = argumentCIs.length;
//		String[] argumentNames = new String[size];
//
//		for (int i = 0; i < size; i++) {
//			argumentNames[i] = argumentCIs[i].getName();
//		}
//
//		return argumentNames;
	}

	private static String argumentNamesToString(String[] argumentNames) {

		StringBuilder buf = new StringBuilder();
		buf.append("(");

		if (argumentNames != null) {

			for (int i = 0; i < argumentNames.length; i++) {

				if (i > 0) {
					buf.append(", ");
				}

				buf.append(argumentNames[i]);
			}
		}

		buf.append(")");
		return buf.toString();
	}
}
