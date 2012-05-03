package ch.usi.dag.disl.snippet;

import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;


public class Shadow {

	protected ClassNode classNode;
	protected MethodNode methodNode;
	protected Snippet snippet;
	
	private AbstractInsnNode regionStart;
	private List<AbstractInsnNode> regionEnds;
	
	private AbstractInsnNode afterThrowStart;
	private AbstractInsnNode afterThrowEnd;
	
	public Shadow(ClassNode classNode, MethodNode methodNode, Snippet snippet,
			AbstractInsnNode regionStart, List<AbstractInsnNode> regionEnds,
			AbstractInsnNode afterThrowStart, AbstractInsnNode afterThrowEnd) {
		super();
		this.classNode = classNode;
		this.methodNode = methodNode;
		this.snippet = snippet;
		this.regionStart = regionStart;
		this.regionEnds = regionEnds;
		this.afterThrowStart = afterThrowStart;
		this.afterThrowEnd = afterThrowEnd;
	}

	// special constructor for caching support
	public Shadow(Shadow sa) {
		
		this.classNode = sa.classNode;
		this.methodNode = sa.methodNode;
		this.snippet = sa.snippet;
		this.regionStart = sa.regionStart;
		this.regionEnds = sa.regionEnds;
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

	public AbstractInsnNode getRegionStart() {
		return regionStart;
	}

	public List<AbstractInsnNode> getRegionEnds() {
		return regionEnds;
	}

	public AbstractInsnNode getAfterThrowStart() {
		return afterThrowStart;
	}

	public AbstractInsnNode getAfterThrowEnd() {
		return afterThrowEnd;
	}
}
