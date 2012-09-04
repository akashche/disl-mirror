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

	// TODO ! is this implementation of methods really working ??
	
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

		methods = new ArrayList<MethodInfo>(classNode.methods.size());
		public_methods = new LinkedList<MethodInfo>();

		for (MethodNode methodNode : classNode.methods) {

			MethodInfo methodInfo = new CommonMethodInfo(methodNode);
			methods.add(methodInfo);

			if (methodInfo.isPublic()) {
				public_methods.add(methodInfo);
			}
		}

		fields = new ArrayList<FieldInfo>(classNode.fields.size());
		public_fields = new LinkedList<FieldInfo>();

		for (FieldNode fieldNode : classNode.fields) {

			FieldInfo fieldInfo = new CommonFieldInfo(fieldNode);
			fields.add(fieldInfo);

			if (fieldInfo.isPublic()) {
				public_fields.add(fieldInfo);
			}
		}

		if (getSuperclass() != null) {

			for (MethodInfo methodInfo : getSuperclass().getMethods()) {
				public_methods.add(methodInfo);
			}

			for (FieldInfo fieldInfo : getSuperclass().getFields()) {
				public_fields.add(fieldInfo);
			}
		}

		interfaces = new ArrayList<String>(classNode.interfaces);
		innerclasses = new ArrayList<String>(classNode.innerClasses.size());

		for (InnerClassNode innerClassNode : classNode.innerClasses) {
			innerclasses.add(innerClassNode.name);
		}
	}

	private int access;
	private String name;

	private List<MethodInfo> methods;
	private List<MethodInfo> public_methods;
	private List<FieldInfo> fields;
	private List<FieldInfo> public_fields;
	private List<String> interfaces;
	private List<String> innerclasses;

	// All these methods should return the value of fields initialized by
	// generateClassInfo(byte[])
	// NOTE: returned arrays should be copies (obtained using
	// Arrays.copyOf(...))

	public boolean isInstance(NetReference nr) {
//		ClassInfo current = ClassInfoResolver.getClass(nr.getClassId());
//
//		while (current != null) {
//
//			if (this == current) {
//				return true;
//			}
//
//			current = current.getSuperclass();
//		}

		// TODO isInstance
		throw new DiSLREServerFatalException("Not implemented");
	}

	public boolean isAssignableFrom(ClassInfo ci) {
		
		// TODO isAssignableFrom
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
		
		// TODO getCanonicalName
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String[] getInterfaces() {

		// to have "checked" array :(
		return interfaces.toArray(new String[0]);
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
		
		// to have "checked" array :(
		return public_fields.toArray(new FieldInfo[0]);
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
		
		// to have "checked" array :(
		return public_methods.toArray(new MethodInfo[0]);
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
		return (String[]) innerclasses.toArray();
	}

	public FieldInfo[] getDeclaredFields() {
		return (FieldInfo[]) fields.toArray();
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
		return (MethodInfo[]) methods.toArray();
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

		if (argumentCIs == null) {
			return new String[0];
		}

		int size = argumentCIs.length;
		String[] argumentNames = new String[size];

		for (int i = 0; i < size; i++) {
			argumentNames[i] = argumentCIs[i].getName();
		}

		return argumentNames;
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
