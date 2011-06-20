package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;

public class MarkedRegion {
	public AbstractInsnNode start;
	public List<AbstractInsnNode> ends;

	public MarkedRegion() {
		this.start = null;
		this.ends = new LinkedList<AbstractInsnNode>();
	}

	public MarkedRegion(AbstractInsnNode start, AbstractInsnNode end) {
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		this.ends.add(end);
	}

	public void addExitPoint(AbstractInsnNode exitpoint) {
		this.ends.add(exitpoint);
	}
}
