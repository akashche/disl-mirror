package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MarkedRegion {
	
	private MethodNode methodnode;
	private AbstractInsnNode start;
	private List<AbstractInsnNode> ends;
	private List<AbstractInsnNode> weaving_ends;

	public MethodNode getMethodnode() {
		return methodnode;
	}

	public AbstractInsnNode getStart() {
		return start;
	}

	public List<AbstractInsnNode> getEnds() {
		return ends;
	}

	public void setStart(AbstractInsnNode start) {
		this.start = start;
	}
	
	public MarkedRegion(MethodNode methodnode, AbstractInsnNode start) {
		this.methodnode = methodnode;
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		weaving_ends = new LinkedList<AbstractInsnNode>();
	}
	
	public MarkedRegion(MethodNode methodnode, AbstractInsnNode start,
			AbstractInsnNode end) {
		this.methodnode = methodnode;
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		this.ends.add(end);
		
		weaving_ends = new LinkedList<AbstractInsnNode>();
	}

	public void addExitPoint(AbstractInsnNode exitpoint) {
		this.ends.add(exitpoint);
	}

	public void addWeavingExitPoint(AbstractInsnNode exitpoint) {
		this.weaving_ends.add(exitpoint);
	}
	
	public List<AbstractInsnNode> getWeavingEnds() {
		return weaving_ends;
	}
}
