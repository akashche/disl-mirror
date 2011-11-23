package ch.usi.dag.disl.example.jp2;

import java.util.*;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;
import ch.usi.dag.disl.util.BasicBlockCalc;

public class JP2Analysis extends AbstractStaticContext {
	
	
	
	public int getNumberOfBBs() {
		return BasicBlockCalc.getAll(staticContextData.getMethodNode().instructions,
				staticContextData.getMethodNode().tryCatchBlocks, false).size();
	
	}
		
	
	public int getInMethodIndex() {
		
		int idx = 0;
		
		InsnList inslist = staticContextData.getMethodNode().instructions;
		AbstractInsnNode instr = staticContextData.getRegionStart();
		
		
		Iterator<AbstractInsnNode> it=inslist.iterator();
		while(it.hasNext()) {
			if(it.next().equals(instr))
				return idx;
			idx++;
		}
		
		return idx;
	}
	
	public boolean isCallToObjectConstructor() {
		
			AbstractInsnNode instr = staticContextData.getRegionStart();

			if (instr.getOpcode() == Opcodes.INVOKESPECIAL) {
				if( ((MethodInsnNode) instr).owner.equals("java/lang/Object") && 
						((MethodInsnNode) instr).name.equals("<init>"))
					return true;
			}
			
			return false;
	}
}

