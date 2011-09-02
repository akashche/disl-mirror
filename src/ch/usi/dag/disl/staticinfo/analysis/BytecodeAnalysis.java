package ch.usi.dag.disl.staticinfo.analysis;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;

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
