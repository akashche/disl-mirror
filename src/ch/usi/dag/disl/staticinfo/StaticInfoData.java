package ch.usi.dag.disl.staticinfo;

import java.util.List;

import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

public class StaticInfoData {

	private ClassNode classNode;
	private ClassNode methodNode;
	private Snippet snippet;
	private List<MarkedRegion> marking;
	private MarkedRegion markedRegion;
	
	public StaticInfoData(ClassNode classNode, ClassNode methodNode,
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

	public ClassNode getMethodNode() {
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
