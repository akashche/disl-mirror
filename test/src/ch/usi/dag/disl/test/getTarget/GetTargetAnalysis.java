package ch.usi.dag.disl.test.getTarget;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.staticinfo.analysis.AbstractStaticAnalysis;

public class GetTargetAnalysis extends AbstractStaticAnalysis {

	public GetTargetAnalysis() {
		super();
	}

	public GetTargetAnalysis(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		super(classNode, methodNode, snippet, markedRegion);
	}

	public boolean isCalleeStatic() {

		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
				.getStart();

		return instr.getOpcode() == Opcodes.INVOKESTATIC;
	}

	public int calleeParCount() {

		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
				.getStart();

		if (!(instr instanceof MethodInsnNode)) {
			return 0;
		}

		return Type.getArgumentTypes(((MethodInsnNode) instr).desc).length;
	}
}
