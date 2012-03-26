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

import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfo {

	private int classId;
	private String classSignature;
	private String classGenericStr;
	private boolean classIsArray;
	private int arrayDimensions;
	private ClassInfo arrayComponentType;
	private NetReference classLoaderNR;
	private ClassInfo superClassInfo;

	ClassInfo(int classId, String classSignature, String classGenericStr,
			boolean classIsArray, int arrayDimensions,
			ClassInfo arrayComponentType, NetReference classLoaderNR,
			ClassInfo superClassInfo, byte[] classCode) {
		super();
		this.classId = classId;
		this.classSignature = classSignature;
		this.classGenericStr = classGenericStr;
		this.classIsArray = classIsArray;
		this.arrayDimensions = arrayDimensions;
		this.arrayComponentType = arrayComponentType;
		this.classLoaderNR = classLoaderNR;
		this.superClassInfo = superClassInfo;

		initializeClassInfo(classCode);
	}

	public int getId() {
		return classId;
	}

	public String getSignature() {
		return classSignature;
	}

	public String getGenericStr() {
		return classGenericStr;
	}

	public boolean isArray() {
		return classIsArray;
	}

	public int getArrayDimensions() {
		return arrayDimensions;
	}

	public ClassInfo getComponentType() {
		return arrayComponentType;
	}

	public NetReference getClassLoaderNR() {
		return classLoaderNR;
	}

	public ClassInfo getSuperClassInfo() {
		return superClassInfo;
	}

	private void initializeClassInfo(byte[] classCode) {
		// *) parse classCode using ASM
		// *) initialize all fields required for the following methods
		if (classCode.length == 0) {

			access = Opcodes.ACC_PUBLIC;
			name = classSignature;
			classIsPrimitive = !classIsArray;

			methods = new ArrayList<MethodInfo>();
			public_methods = new LinkedList<MethodInfo>();
			fields = new ArrayList<FieldInfo>();
			public_fields = new LinkedList<FieldInfo>();
			interfaces = new ArrayList<String>();
			innerclasses = new ArrayList<String>();
		} else {

			ClassReader classReader = new ClassReader(classCode);
			ClassNode classNode = new ClassNode(Opcodes.ASM4);
			classReader.accept(classNode, ClassReader.SKIP_DEBUG
					| ClassReader.EXPAND_FRAMES);

			access = classNode.access;
			name = classNode.name.replace('/', '.');
			classIsPrimitive = false;

			methods = new ArrayList<MethodInfo>(classNode.methods.size());
			public_methods = new LinkedList<MethodInfo>();

			for (MethodNode methodNode : classNode.methods) {

				MethodInfo methodInfo = new MethodInfo(methodNode);
				methods.add(methodInfo);

				if (methodInfo.isPublic()) {
					public_methods.add(methodInfo);
				}
			}

			fields = new ArrayList<FieldInfo>(classNode.fields.size());
			public_fields = new LinkedList<FieldInfo>();

			for (FieldNode fieldNode : classNode.fields) {

				FieldInfo fieldInfo = new FieldInfo(fieldNode);
				fields.add(fieldInfo);

				if (fieldInfo.isPublic()) {
					public_fields.add(fieldInfo);
				}
			}

			if (superClassInfo != null) {

				for (MethodInfo methodInfo : superClassInfo.getMethods()) {
					public_methods.add(methodInfo);
				}

				for (FieldInfo fieldInfo : superClassInfo.getFields()) {
					public_fields.add(fieldInfo);
				}
			}

			interfaces = new ArrayList<String>(classNode.interfaces);
			innerclasses = new ArrayList<String>(classNode.innerClasses.size());

			for (InnerClassNode innerClassNode : classNode.innerClasses) {
				innerclasses.add(innerClassNode.name);
			}
		}
	}

	private int access;
	private String name;
	private boolean classIsPrimitive;

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

	// TODO isInstance
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

		return false;
	}

	// TODO isAssignableFrom
	public boolean isAssignableFrom(ClassInfo ci) {
		return false;
	}

	public boolean isInterface() {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}

	public boolean isPrimitive() {
		return classIsPrimitive;
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

	// TODO getCanonicalName
	public String getCanonicalName() {
		return null;
	}

	public String[] getInterfaces() {
		return (String[]) interfaces.toArray();
	}

	public String getPackage() {
		int i = name.lastIndexOf('.');

		if (i != -1) {
			return name.substring(0, i);
		} else {
			return null;
		}
	}

	public ClassInfo getSuperclass() {
		return superClassInfo;
	}

	public FieldInfo[] getFields() {
		return (FieldInfo[]) public_fields.toArray();
	}

	public FieldInfo getField(String fieldName) throws NoSuchFieldException {

		for (FieldInfo fieldInfo : fields) {
			if (fieldInfo.isPublic() && fieldInfo.getName().equals(fieldName)) {
				return fieldInfo;
			}
		}

		if (superClassInfo == null) {
			throw new NoSuchFieldException(name + "." + fieldName);
		}

		return superClassInfo.getField(fieldName);
	}

	public MethodInfo[] getMethods() {
		return (MethodInfo[]) public_methods.toArray();
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

		if (superClassInfo == null) {
			throw new NoSuchMethodException(name + "." + methodName
					+ argumentNamesToString(argumentNames));
		}

		return superClassInfo.getMethod(methodName, argumentNames);
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
