package ch.usi.dag.dislreserver.shadow;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

class ShadowCommonClass extends ShadowClass {

	private ShadowClass superClass;
	private ClassNode classNode;

	private int access;
	private String name;

	private String classGenericStr;

	ShadowCommonClass(long net_ref, String classSignature,
			String classGenericStr, ShadowObject classLoader,
			ShadowClass superClass, byte[] classCode) {
		super(net_ref, classLoader);

		this.classGenericStr = classGenericStr;
		this.superClass = superClass;

		ClassReader classReader = new ClassReader(classCode);
		classNode = new ClassNode(Opcodes.ASM4);
		classReader.accept(classNode, ClassReader.SKIP_DEBUG
				| ClassReader.EXPAND_FRAMES);

		access = classNode.access;
		name = classNode.name.replace('/', '.');
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
		// TODO consider interfaces
		return equals(obj.getSClass());
	}

	@Override
	public boolean isAssignableFrom(ShadowClass klass) {
		// TODO consider interfaces

		while (klass != null) {

			if (klass.equals(this)) {
				return true;
			}

			klass = klass.getSuperclass();
		}

		return false;
	}

	@Override
	public boolean isInterface() {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public boolean isPrimitive() {
		return false;
	}

	@Override
	public boolean isAnnotation() {
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}

	@Override
	public boolean isSynthetic() {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}

	@Override
	public boolean isEnum() {
		return (access & Opcodes.ACC_ENUM) != 0;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCanonicalName() {
		return name;
	}

	@Override
	public String[] getInterfaces() {

		if (isInterface()) {
			return new String[] { classGenericStr };
		} else {
			int index = classGenericStr.indexOf(':');

			if (index == -1) {
				return new String[0];
			} else {
				String interfaces = classGenericStr.substring(index + 1);
				return interfaces.split(":");
			}
		}
	}

	@Override
	public String getPackage() {

		int i = name.lastIndexOf('.');

		if (i != -1) {
			return name.substring(0, i);
		} else {
			return null;
		}
	}

	@Override
	public ShadowClass getSuperclass() {
		return superClass;
	}

}
