package ch.usi.dag.disl.staticcontext;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;

public class BytecodeAnalysis extends AbstractStaticAnalysis {

	public BytecodeAnalysis() {
		super();
	}

	public BytecodeAnalysis(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		super(classNode, methodNode, snippet, markedRegion);
	}

	public int getBytecodeNumber() {
		
		return staticAnalysisData.getMarkedRegion().getStart().getOpcode();
	}
}
