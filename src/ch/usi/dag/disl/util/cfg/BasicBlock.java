package ch.usi.dag.disl.util.cfg;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;

public class BasicBlock {
	private AbstractInsnNode entrance;
	private AbstractInsnNode exit;

	private Set<BasicBlock> predecessors;
	private Set<BasicBlock> successors;

	public BasicBlock(AbstractInsnNode entrance, AbstractInsnNode exit) {
		this.entrance = entrance;
		this.exit = exit;
		this.successors = new HashSet<BasicBlock>();
		this.predecessors = new HashSet<BasicBlock>();
	}

	public AbstractInsnNode getEntrance() {
		return entrance;
	}

	public AbstractInsnNode getExit() {
		return exit;
	}

	public Set<BasicBlock> getPredecessors() {
		return predecessors;
	}

	public Set<BasicBlock> getSuccessors() {
		return successors;
	}
}
