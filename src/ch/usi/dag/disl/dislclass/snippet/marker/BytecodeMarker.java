package ch.usi.dag.disl.dislclass.snippet.marker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.util.AsmOpcodes;
import ch.usi.dag.disl.util.Parameter;

public class BytecodeMarker implements Marker {

	protected static final String INSTR_DELIM = ",";
	
	protected Set<Integer> searchedInstrNums = new HashSet<Integer>();
	
	public BytecodeMarker(Parameter param) throws MarkerException {
		
		// set delim for instruction list
		param.setMultipleValDelim(INSTR_DELIM);
		
		// translate all instructions to opcodes 
		for(String instr : param.getMultipleValues()) {
			
			try {
				
				AsmOpcodes opcode = 
					AsmOpcodes.valueOf(instr.trim().toUpperCase());
				searchedInstrNums.add(opcode.getNumber());
			}
			catch(IllegalArgumentException e) {
				
				throw new MarkerException("Instruction \"" + instr +
						"\" cannot be found. See " +
						AsmOpcodes.class.getName() +
						" enum for list of possible instructions");
			}
		}
	}

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		InsnList ilst = method.instructions;

		for (AbstractInsnNode instruction : ilst.toArray()) {
			
			if (searchedInstrNums.contains(instruction.getOpcode())) {
				
				regions.add(new MarkedRegion(method, instruction, instruction));
			}
		}

		return regions;
	}
}
