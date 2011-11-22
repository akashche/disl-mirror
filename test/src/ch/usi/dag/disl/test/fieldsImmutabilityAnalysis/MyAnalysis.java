package ch.usi.dag.disl.test.fieldsImmutabilityAnalysis;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class MyAnalysis extends AbstractStaticContext {
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

	public String getAccessedObjectClassName() {
		AbstractInsnNode instr = staticContextData.getRegionStart();

		if (instr.getOpcode() == Opcodes.GETSTATIC
				|| instr.getOpcode() == Opcodes.PUTSTATIC) {
			return  ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
		} else {
			return "ERROR!";
		}
	}
	
	public String getAccessedFieldsName() {
		AbstractInsnNode instr = staticContextData.getRegionStart();

		if (instr.getOpcode() == Opcodes.PUTFIELD
				|| instr.getOpcode() == Opcodes.GETFIELD) {
			return  ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
		} else {
			return "ERROR!";
		}
	}
}

