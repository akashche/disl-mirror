package ch.usi.dag.disl.test.jp2;

import java.util.*;


import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import ch.usi.dag.disl.staticcontext.AbstractStaticAnalysis;
import ch.usi.dag.disl.util.BasicBlockCalc;

public class JP2Analysis extends AbstractStaticAnalysis {
	
	
	
	public int getNumberOfBBs() {
		return BasicBlockCalc.getAll(staticAnalysisData.getMethodNode().instructions,
				staticAnalysisData.getMethodNode().tryCatchBlocks, false).size();
	
	}
		
	
	public int getInMethodIndex() {
		
		int idx = 0;
		
		InsnList inslist = staticAnalysisData.getMethodNode().instructions;
		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
		.getStart();
		
		
		Iterator<AbstractInsnNode> it=inslist.iterator();
		while(it.hasNext()) {
			if(it.next().equals(instr))
				return idx;
			idx++;
		}
		
		return idx;
	}
	
	public boolean isCallToObjectConstructor() {
		
			AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
					.getStart();

			if (instr.getOpcode() == Opcodes.INVOKESPECIAL) {
				if( ((MethodInsnNode) instr).owner.equals("java/lang/Object") && 
						((MethodInsnNode) instr).name.equals("<init>"))
					return true;
			}
			
			return false;
	}
}

