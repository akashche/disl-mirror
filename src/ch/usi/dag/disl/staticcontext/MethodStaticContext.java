package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.Opcodes;

import ch.usi.dag.disl.util.Constants;

public class MethodStaticContext extends AbstractStaticContext {

	// *** Class ***

	public String thisClassName() {

		return staticContextData.getClassNode().name;
	}
	
	public String thisClassCanonicalName() {

		return staticContextData.getClassNode().name.replace('/', '.');
	}
	
	public String thisClassOuterClass() {

		return staticContextData.getClassNode().outerClass;
	}
	
	public String thisClassOuterMethod() {

		return staticContextData.getClassNode().outerMethod;
	}
	
	public String thisClassOuterMethodDesc() {

		return staticContextData.getClassNode().outerMethodDesc;
	}
	
	public String thisClassSignature() {

		return staticContextData.getClassNode().signature;
	}
	
	public String thisClassSourceFile() {

		return staticContextData.getClassNode().sourceFile;
	}
	
	public String thisClassSuperName() {

		return staticContextData.getClassNode().superName;
	}
	
	public int thisClassVersion() {

		return staticContextData.getClassNode().version;
	}
	
	public boolean isClassAbstract() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_ABSTRACT) != 0;
	}
	
	public boolean isClassAnnotation() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_ANNOTATION) != 0;
	}

	public boolean isClassEnum() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_ENUM) != 0;
	}
	
	public boolean isClassFinal() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_FINAL) != 0;
	}
	
	public boolean isClassInterface() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}
	
	public boolean isClassPrivate() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_PRIVATE) != 0;
	}
	
	public boolean isClassProtected() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_PROTECTED) != 0;
	}
	
	public boolean isClassPublic() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}
	
	public boolean isClassSynthetic() {

		int access = staticContextData.getClassNode().access;
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}
	
	// *** Method ***
	
	public String thisMethodName() {

		return staticContextData.getMethodNode().name;
	}

	public String thisMethodFullName() {

		return staticContextData.getClassNode().name
				+ Constants.STATIC_CONTEXT_METHOD_DELIM
				+ staticContextData.getMethodNode().name;
	}
	
	public String thisMethodDescriptor() {
		
		return staticContextData.getMethodNode().desc;
	}
	
	public String thisMethodSignature() {
		
		return staticContextData.getMethodNode().signature;
	}
	
	public boolean isMethodBridge() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}
	
	public boolean isMethodFinal() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_FINAL) != 0;
	}
	
	public boolean isMethodPrivate() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_PRIVATE) != 0;
	}
	
	public boolean isMethodProtected() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_PROTECTED) != 0;
	}
	
	public boolean isMethodPublic() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_PUBLIC) != 0;
	}
	
	public boolean isMethodStatic() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_STATIC) != 0;
	}
	
	public boolean isMethodSynchronized() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
	}
	
	public boolean isMethodVarArgs() {
		
		int access = staticContextData.getMethodNode().access;
		return (access & Opcodes.ACC_VARARGS) != 0;
	}
}
