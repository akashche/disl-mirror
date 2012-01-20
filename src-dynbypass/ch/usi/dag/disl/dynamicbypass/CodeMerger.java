package ch.usi.dag.disl.dynamicbypass;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class CodeMerger {

	private static final String METHOD_FINALIZE = "finalize";

	private static final String DBCHECK_CLASS = Type.getInternalName(
			DynamicBypassCheck.class);
	private static final String DBCHECK_METHOD = "executeUninstrumented";
	private static final String DBCHECK_DESC = "()Z";

	// NOTE: the instrumented class node will serve as an output
	public static void mergeClasses(ClassNode originalCN,
			ClassNode instrumentedCN) {

		// do not merge interfaces
		if ((originalCN.access & Opcodes.ACC_INTERFACE) != 0) {
			return;
		}

		for (MethodNode instrMN : instrumentedCN.methods) {

			// evaluation is done always but is more visible then in single if

			boolean methodAbstract = (instrMN.access & Opcodes.ACC_ABSTRACT) != 0;
			boolean methodNative = (instrMN.access & Opcodes.ACC_NATIVE) != 0;
			// TODO jb ! test
			boolean methodFinalizeInObject = instrumentedCN.name.equals(Type
					.getInternalName(Object.class))
					&& instrMN.name.equals(METHOD_FINALIZE);

			// skip methods that should not be merged
			if (methodAbstract | methodNative | methodFinalizeInObject) {
				continue;
			}

			MethodNode origMN = getMethodNode(originalCN, instrMN.name,
					instrMN.desc);

			InsnList ilist = instrMN.instructions;

			// TODO jb - check for similarity of the instructions

			// add reference to the original code
			LabelNode origCodeL = new LabelNode();
			ilist.add(origCodeL);

			// add original code
			ilist.add(origMN.instructions);
			// add exception handlers of the original code
			instrMN.tryCatchBlocks.addAll(origMN.tryCatchBlocks);

			// if the dynamic bypass is activated (non-zero value returned)
			// then jump to original code
			ilist.insert(new JumpInsnNode(Opcodes.IFNE, origCodeL));
			ilist.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
					DBCHECK_CLASS, DBCHECK_METHOD, DBCHECK_DESC));
		}
	}

	private static MethodNode getMethodNode(ClassNode cnToSearch,
			String methodName, String methodDesc) {

		for (MethodNode mn : cnToSearch.methods) {
			if (methodName.equals(mn.name) && methodDesc.equals(mn.desc)) {
				return mn;
			}
		}

		throw new RuntimeException(
				"Code merger fatal error: method for merge not found");
	}
}
