package ch.usi.dag.dislreserver.reflectiveinfo;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class CommonMethodInfo implements MethodInfo {

	private MethodNode methodNode;
	private int modifiers;
	private String name;
	private String returnType;
	private String[] parameterTypes;
	private String[] exceptionTypes;

	public CommonMethodInfo(MethodNode methodNode) {

		this.methodNode = methodNode;
		name = methodNode.name;
		modifiers = methodNode.access;
		returnType = Type.getReturnType(methodNode.desc).getDescriptor();

		Type[] parameters = Type.getArgumentTypes(methodNode.desc);
		int size = parameters.length;
		parameterTypes = new String[size];

		for (int i = 0; i < size; i++) {
			parameterTypes[i] = parameters[i].getDescriptor();
		}

		// to have "checked" array :(
		exceptionTypes = methodNode.exceptions.toArray(new String[0]);
	}

	public MethodNode getMethodNode() {
		return methodNode;
	}

	
	public String getName() {
		return name;
	}

	public int getModifiers() {
		// TODO Auto-generated method stub
		throw new DiSLREServerFatalException("Not implemented");
	}

	public String getReturnType() {
		return returnType;
	}

	public String[] getParameterTypes() {
		return Arrays.copyOf(parameterTypes, parameterTypes.length);
	}

	public String[] getExceptionTypes() {
		return Arrays.copyOf(exceptionTypes, exceptionTypes.length);
	}

	public boolean isPublic() {
		return (modifiers & Opcodes.ACC_PUBLIC) != 0;
	}
}
