package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// If 'end' is the previous instruction of 'start', then the basic block
	// contains only one branch instruction.
	public static int isPrevious(AbstractInsnNode end, AbstractInsnNode start) {
		// This happens only when the first instruction is a branch instruction.
		if (end == null)
			return 0;

		// Skip labels and compare the first not-label one with 'end'.
		AbstractInsnNode iterator = start.getPrevious();
		int label_count = 0;

		while (iterator != null) {
			if (iterator.getOpcode() == -1) {
				iterator = iterator.getPrevious();
				label_count++;
			} else {
				return iterator == end ? label_count : -1;
			}
		}

		return -1;
	}

	// Fix the stack operand index of each stack-based instruction
	// according to the maximum number of locals in the target method node.
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void fixLocalIndex(MethodNode method, InsnList src) {
		int max = method.maxLocals;

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr instanceof VarInsnNode) {
				VarInsnNode varInstr = (VarInsnNode) instr;
				varInstr.var += method.maxLocals;

				if (varInstr.var > max) {
					max = varInstr.var;
				}
			}
		}

		method.maxLocals = max;
	}

	// Transform static fields to synthetic local
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void static2Local(MethodNode method,
			List<SyntheticLocalVar> syntheticLocalVars) {

		// Extract the 'id' field in each synthetic local
		List<String> id_set = new LinkedList<String>();
		AbstractInsnNode first = method.instructions.getFirst();

		// Initialization
		for (SyntheticLocalVar var : syntheticLocalVars) {
			
			if (var.getInitASMCode() != null) {
				InsnList newlst = InsnListHelper
						.cloneList(var.getInitASMCode());
				method.instructions.insertBefore(first, newlst);
			}

			id_set.add(var.getID());
		}

		// Scan for FIELD instructions and replace with local load/store.
		for (AbstractInsnNode instr : method.instructions.toArray()) {
			int opcode = instr.getOpcode();

			// Only field instructions will be transformed.
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
				FieldInsnNode field_instr = (FieldInsnNode) instr;
				String id = field_instr.owner + SyntheticLocalVar.NAME_DELIM
						+ field_instr.name;
				int index = id_set.indexOf(id);

				if (index == -1) {
					// Not contained in the synthetic locals.
					continue;
				}

				// Construct a instruction for local
				Type type = Type.getType(field_instr.desc);
				int new_opcode = type
						.getOpcode(opcode == Opcodes.GETSTATIC ? Opcodes.ILOAD
								: Opcodes.ISTORE);
				VarInsnNode insn = new VarInsnNode(new_opcode, method.maxLocals
						+ index);
				method.instructions.insertBefore(instr, insn);
				method.instructions.remove(instr);
			}
		}

		method.maxLocals += syntheticLocalVars.size();
	}

	// TODO support for AfterReturning and AfterThrowing
	// TODO support for static information weaving
	public static void instrument(MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars) {
		// Sort the snippets based on their order
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		// Fixing empty region
		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);

			for (MarkedRegion region : regions) {
				// For iterating through the list. NOTE that we are going to
				// remove/add new items into the list.
				AbstractInsnNode ends[] = new AbstractInsnNode[region.getEnds()
						.size()];

				for (AbstractInsnNode exit : region.getEnds().toArray(ends)) {
					AbstractInsnNode start = region.getStart();

					switch (isPrevious(exit, start)) {
					case -1:
						break;
					case 0:
						// No label? Then give them one label!
						region.getMethodnode().instructions.insertBefore(start,
								new LabelNode(new Label()));
					default:
						// Now we have a label between them. Both 'start' and
						// 'end' will set to the label node.
						region.setStart(region.getStart().getPrevious());
						region.getEnds().remove(exit);
						region.addExitPoint(region.getStart());
						break;
					}
				}
			}
		}

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			InsnList ilst = snippet.getAsmCode();

			// skip snippet with empty code
			if (snippet.getAsmCode() == null) {
				continue;
			}

			// Instrument
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {
					InsnList newlst = InsnListHelper.cloneList(ilst);
					fixLocalIndex(region.getMethodnode(), newlst);
					region.getMethodnode().instructions.insertBefore(
							region.getStart(), newlst);
				}
			} else if (snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.getEnds()) {
						InsnList newlst = InsnListHelper.cloneList(ilst);
						fixLocalIndex(region.getMethodnode(), newlst);
						region.getMethodnode().instructions
								.insert(exit, newlst);
					}
				}
			}
		}

		static2Local(methodNode, syntheticLocalVars);
	}
}
