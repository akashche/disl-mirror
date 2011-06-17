package ch.usi.dag.disl.marker;

import org.objectweb.asm.tree.AbstractInsnNode;

public class MarkRegion {
	public AbstractInsnNode start;
	public AbstractInsnNode end;

	public MarkRegion() {
		this.start = null;
		this.end = null;
	}
	
	public MarkRegion(AbstractInsnNode start, AbstractInsnNode end) {
		this.start = start;
		this.end = end;
	}
}
