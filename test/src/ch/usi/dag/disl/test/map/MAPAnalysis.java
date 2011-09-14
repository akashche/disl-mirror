package ch.usi.dag.disl.test.map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import ch.usi.dag.disl.staticinfo.analysis.AbstractStaticAnalysis;

public class MAPAnalysis extends AbstractStaticAnalysis {
	

	public String getFieldName() {

		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
				.getStart();

		if (instr.getOpcode() == Opcodes.GETFIELD
				|| instr.getOpcode() == Opcodes.PUTFIELD
				) {
			return ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
			
		} else {
			return "null";
		}
	}
	
	public String getStaticFieldName() {

		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
				.getStart();

		if (instr.getOpcode() == Opcodes.GETSTATIC
				|| instr.getOpcode() == Opcodes.PUTSTATIC) {
			return  ((FieldInsnNode) instr).owner + "." + ((FieldInsnNode) instr).name;
		} else {
			return "null";
		}
	}
	
	public int getAMultiArrayDimension() {
		
		AbstractInsnNode instr = staticAnalysisData.getMarkedRegion()
			.getStart();
		
		if(instr.getOpcode() == Opcodes.MULTIANEWARRAY) {
			return ((MultiANewArrayInsnNode)instr).dims;
		}else{
			return 0;
		}
	}
}
