package ch.usi.dag.disl.utilinstr.codemerger;

import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dynamicbypass.DynamicBypassCheck;
import ch.usi.dag.disl.exception.DiSLFatalException;

public abstract class CodeMerger {

	private static final String DBCHECK_CLASS = Type.getInternalName(
			DynamicBypassCheck.class);
	private static final String DBCHECK_METHOD = "executeUninstrumented";
	private static final String DBCHECK_DESC = "()Z";

	// NOTE: the originalCN and instrumentedCN will be destroyed in the process
	// NOTE: abstract or native methods should not be included in the
	//       changedMethods list
	public static ClassNode mergeClasses(ClassNode originalCN,
			ClassNode instrumentedCN, Set<MethodNode> changedMethods) {

		// NOTE: that instrumentedCN can contain added fields
		//       - has to be returned
		
		if(changedMethods == null) {
			throw new DiSLFatalException(
					"Set of changed methods cannot be null");
		}

		// no changed method - no merging
		 if(changedMethods.isEmpty()) {
			 return instrumentedCN;
		 }

		 // TODO jb ! add splitting for to long methods
		 //  - splitting is off by default
		 //  - measure the length of the resulting method and split if necessary
		 //  - ignore clinit - output warning
		 //  - output warning if splitted is to large and ignore
		 
		for (MethodNode instrMN : instrumentedCN.methods) {
			
			// We will construct the merged method node in the instrumented
			// class node
			
			// skip unchanged methods 
			if(! changedMethods.contains(instrMN)) {
				continue;
			}

			MethodNode origMN = getMethodNode(originalCN, instrMN.name,
					instrMN.desc);

			InsnList ilist = instrMN.instructions;

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
		
		return instrumentedCN;
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
