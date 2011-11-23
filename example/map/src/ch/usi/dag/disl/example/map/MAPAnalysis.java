package ch.usi.dag.disl.example.map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class MAPAnalysis extends AbstractStaticContext {
	

	public String getFieldName() {

		AbstractInsnNode instr = staticContextData.getRegionStart();

		if (instr.getOpcode() == Opcodes.GETFIELD
				|| instr.getOpcode() == Opcodes.PUTFIELD
				) {
			return ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
			
		} else {
			return "null";
		}
	}
	
	public String getStaticFieldName() {

		AbstractInsnNode instr = staticContextData.getRegionStart();

		if (instr.getOpcode() == Opcodes.GETSTATIC
				|| instr.getOpcode() == Opcodes.PUTSTATIC) {
			return  ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
		} else {
			return "null";
		}
	}
	
	public int getAMultiArrayDimension() {
		
		AbstractInsnNode instr = staticContextData.getRegionStart();
		
		if(instr.getOpcode() == Opcodes.MULTIANEWARRAY) {
			return ((MultiANewArrayInsnNode)instr).dims;
		}else{
			return 0;
		}
	}
}
