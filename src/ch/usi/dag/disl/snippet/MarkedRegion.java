package ch.usi.dag.disl.snippet;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

public class MarkedRegion {

	private AbstractInsnNode start;
	private List<AbstractInsnNode> ends;

	public AbstractInsnNode getStart() {
		return start;
	}

	public List<AbstractInsnNode> getEnds() {
		return ends;
	}

	public void setStart(AbstractInsnNode start) {
		this.start = start;
	}

	public MarkedRegion(AbstractInsnNode start) {
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
	}

	public MarkedRegion(AbstractInsnNode start, AbstractInsnNode end) {
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		this.ends.add(end);
	}

	public MarkedRegion(AbstractInsnNode start,	List<AbstractInsnNode> ends) {
		this.start = start;
		this.ends = ends;
	}

	public void addExitPoint(AbstractInsnNode exitpoint) {
		this.ends.add(exitpoint);
	}
}
