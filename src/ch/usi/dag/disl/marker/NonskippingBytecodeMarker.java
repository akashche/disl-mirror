package ch.usi.dag.disl.marker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.util.AsmOpcodes;

public class NonskippingBytecodeMarker extends AbstractInsnMarker {

	protected Set<Integer> searchedInstrNums = new HashSet<Integer>();

	public NonskippingBytecodeMarker(Parameter param) throws MarkerException {

		// translate all instructions to opcodes
		for (String instr : param.getMultipleValues()) {

			try {

				AsmOpcodes opcode = AsmOpcodes.valueOf(instr.trim()
						.toUpperCase());
				searchedInstrNums.add(opcode.getNumber());
			} catch (IllegalArgumentException e) {

				throw new MarkerException("Instruction \"" + instr
						+ "\" cannot be found. See "
						+ AsmOpcodes.class.getName()
						+ " enum for list of possible instructions");
			}
		}

		if (searchedInstrNums.isEmpty()) {
			throw new MarkerException("Bytecode marker cannot operate without"
					+ " selected instructions. Pass instruction list using"
					+ " \"param\" annotation attribute.");
		}
	}

	@Override
	public List<AbstractInsnNode> markInstruction(MethodNode method) {

		List<AbstractInsnNode> seleted = new LinkedList<AbstractInsnNode>();

		for (AbstractInsnNode instruction : method.instructions.toArray()) {

			if (searchedInstrNums.contains(instruction.getOpcode())) {

				seleted.add(instruction);
			}
		}

		return seleted;
	}
}
