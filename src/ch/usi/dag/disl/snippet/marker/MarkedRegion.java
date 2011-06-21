package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class MarkedRegion {
	public MethodNode methodnode;
	public AbstractInsnNode start;
	public List<AbstractInsnNode> ends;

	public MarkedRegion(MethodNode methodnode) {
		this.methodnode = methodnode;
		this.start = null;
		this.ends = new LinkedList<AbstractInsnNode>();
	}

	public MarkedRegion(MethodNode methodnode, AbstractInsnNode start,
			AbstractInsnNode end) {
		this.methodnode = methodnode;
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		this.ends.add(end);
	}

	public void addExitPoint(AbstractInsnNode exitpoint) {
		this.ends.add(exitpoint);
	}
}
