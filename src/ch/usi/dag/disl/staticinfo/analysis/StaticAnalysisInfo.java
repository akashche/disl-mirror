package ch.usi.dag.disl.staticinfo.analysis;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

public class StaticAnalysisInfo {

	private ClassNode classNode;
	private MethodNode methodNode;
	private Snippet snippet;
	private List<MarkedRegion> marking;
	private MarkedRegion markedRegion;
	
	public StaticAnalysisInfo(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, List<MarkedRegion> marking,
			MarkedRegion markedRegion) {
		super();
		this.classNode = classNode;
		this.methodNode = methodNode;
		this.snippet = snippet;
		this.marking = marking;
		this.markedRegion = markedRegion;
	}

	public ClassNode getClassNode() {
		return classNode;
	}

	public MethodNode getMethodNode() {
		return methodNode;
	}

	public Snippet getSnippet() {
		return snippet;
	}

	public List<MarkedRegion> getMarking() {
		return marking;
	}

	public MarkedRegion getMarkedRegion() {
		return markedRegion;
	}
}
