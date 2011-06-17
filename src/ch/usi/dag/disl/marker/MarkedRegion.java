package ch.usi.dag.disl.marker;

import org.objectweb.asm.tree.AbstractInsnNode;

public class MarkedRegion {
	public AbstractInsnNode start;
	public AbstractInsnNode end;

	public MarkedRegion() {
		this.start = null;
		this.end = null;
	}
	
	public MarkedRegion(AbstractInsnNode start, AbstractInsnNode end) {
		this.start = start;
		this.end = end;
	}
}
