package ch.usi.dag.disl.util.cfg;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;

public class BasicBlock {
	private int index;
	private AbstractInsnNode entrance;
	private AbstractInsnNode exit;

	private Set<BasicBlock> predecessors;
	private Set<BasicBlock> successors;
	private Set<BasicBlock> joins;
	
	private Set<BasicBlock> dominators;
	
	private boolean loop;

	public BasicBlock(int index, AbstractInsnNode entrance,
			AbstractInsnNode exit) {
		this.index = index;
		this.entrance = entrance;
		this.exit = exit;

		successors = new HashSet<BasicBlock>();
		predecessors = new HashSet<BasicBlock>();
		joins = new HashSet<BasicBlock>();
		
		dominators = new HashSet<BasicBlock>();
		
		loop = false;
	}

	public int getIndex() {
		return index;
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

	public Set<BasicBlock> getJoins() {
		return joins;
	}

	public Set<BasicBlock> getDominators() {
		return dominators;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public boolean isLoop() {
		return loop;
	}

}
