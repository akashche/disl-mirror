package ch.usi.dag.disl.snippet;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;


// TODO ! shadow

public class Shadow {

	protected ClassNode classNode;
	protected MethodNode methodNode;
	protected Snippet snippet;
	protected MarkedRegion markedRegion;
	
	public Shadow(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		super();
		this.classNode = classNode;
		this.methodNode = methodNode;
		this.snippet = snippet;
		this.markedRegion = markedRegion;
	}
	
	// special constructor for caching support
	public Shadow(Shadow sa) {
		
		this.classNode = sa.classNode;
		this.methodNode = sa.methodNode;
		this.snippet = sa.snippet;
		this.markedRegion = sa.markedRegion;
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

	public MarkedRegion getMarkedRegion() {
		return markedRegion;
	}
}
