package ch.usi.dag.disl.staticcontext;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;

public class StaticAnalysisData {

	protected ClassNode classNode;
	protected MethodNode methodNode;
	protected Snippet snippet;
	protected List<MarkedRegion> marking;
	protected MarkedRegion markedRegion;
	
	public StaticAnalysisData(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, List<MarkedRegion> marking,
			MarkedRegion markedRegion) {
		super();
		this.classNode = classNode;
		this.methodNode = methodNode;
		this.snippet = snippet;
		this.marking = marking;
		this.markedRegion = markedRegion;
	}
	
	// special constructor for caching support
	public StaticAnalysisData(StaticAnalysisData sad) {
		
		this.classNode = sad.classNode;
		this.methodNode = sad.methodNode;
		this.snippet = sad.snippet;
		this.marking = sad.marking;
		this.markedRegion = sad.markedRegion;
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
