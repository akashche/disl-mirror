package ch.usi.dag.disl.util.stack;

import java.util.HashSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

public class StackEntry {
	
	private int type = Type.VOID;	
	private HashSet<AbstractInsnNode> list = null;

	public StackEntry() {
		this.list = new HashSet<AbstractInsnNode>();
	}
	
	public StackEntry(int type, AbstractInsnNode instr) {
		this.list = new HashSet<AbstractInsnNode>();
		this.list.add(instr);
		setType(type);
	}

	public void setType(int type) {
		if (this.type != Type.VOID && this.type != type) {
			throw new RuntimeException("Type error");
		}

		this.type = type;
	}

	public boolean add(AbstractInsnNode instr) {
		return list.add(instr);
	}
	
	public StackEntry clone(){
		
		StackEntry entry = new StackEntry();
		entry.list.addAll(list);
		entry.type = type;
		
		return entry;
	}

	public boolean merge(StackEntry stackEntry) {
		
		if (type != stackEntry.type){
			return false;
		}
		
		boolean flag = false;
		
		for (AbstractInsnNode instr : stackEntry.list){
			flag = list.add(instr) || flag;
		}
		
		return flag;
	}

	public int getType() {
		return type;
	}

	public HashSet<AbstractInsnNode> getList() {
		return list;
	}
}
