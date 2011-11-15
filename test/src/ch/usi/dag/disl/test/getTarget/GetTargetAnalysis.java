package ch.usi.dag.disl.test.getTarget;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class GetTargetAnalysis extends AbstractStaticContext {

	public boolean isCalleeStatic() {

		AbstractInsnNode instr = staticContextData.getMarkedRegion()
				.getStart();

		return instr.getOpcode() == Opcodes.INVOKESTATIC;
	}

	public int calleeParCount() {

		AbstractInsnNode instr = staticContextData.getMarkedRegion()
				.getStart();

		if (!(instr instanceof MethodInsnNode)) {
			return 0;
		}

		return Type.getArgumentTypes(((MethodInsnNode) instr).desc).length;
	}
}
