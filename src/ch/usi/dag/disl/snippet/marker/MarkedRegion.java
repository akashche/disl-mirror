package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public class MarkedRegion {
	public InsnList ilst;
	public AbstractInsnNode start;
	public List<AbstractInsnNode> ends;

	public MarkedRegion(InsnList ilst) {
		this.ilst = ilst;
		this.start = null;
		this.ends = new LinkedList<AbstractInsnNode>();
	}

	public MarkedRegion(InsnList ilst, AbstractInsnNode start,
			AbstractInsnNode end) {
		this.ilst = ilst;
		this.start = start;
		this.ends = new LinkedList<AbstractInsnNode>();
		this.ends.add(end);
	}

	public void addExitPoint(AbstractInsnNode exitpoint) {
		this.ends.add(exitpoint);
	}
}
