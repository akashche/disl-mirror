package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	public static List<AbstractInsnNode> fixRegion(MethodNode methodNode,
			MarkedRegion region) {
		List<AbstractInsnNode> weaving_ends = new LinkedList<AbstractInsnNode>();
		AbstractInsnNode start = region.getStart();

		for (AbstractInsnNode end : region.getEnds()) {
			if (InsnListHelper.isBranch(end)) {
				if (start == end) {
					LabelNode labelNode = new LabelNode();
					methodNode.instructions.insertBefore(start, labelNode);
					region.setStart(labelNode);
				}

				weaving_ends.add(end.getPrevious());
			}
		}

		return weaving_ends;
	}

	// Fix the stack operand index of each stack-based instruction
	// according to the maximum number of locals in the target method node.
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void fixLocalIndex(MethodNode methodNode, InsnList src) {
		int max = methodNode.maxLocals;

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr instanceof VarInsnNode) {
				VarInsnNode varInstr = (VarInsnNode) instr;
				varInstr.var += methodNode.maxLocals;

				if (varInstr.var > max) {
					max = varInstr.var;
				}
			}
		}

		methodNode.maxLocals = max;
	}

	// Transform static fields to synthetic local
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void static2Local(MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocalVars) {

		// Extract the 'id' field in each synthetic local
		List<String> id_set = new LinkedList<String>();
		AbstractInsnNode first = methodNode.instructions.getFirst();

		// Initialization
		for (SyntheticLocalVar var : syntheticLocalVars) {

			if (var.getInitASMCode() != null) {
				InsnList newlst = InsnListHelper
						.cloneList(var.getInitASMCode());
				methodNode.instructions.insertBefore(first, newlst);
			}

			id_set.add(var.getID());
		}

		// Scan for FIELD instructions and replace with local load/store.
		for (AbstractInsnNode instr : methodNode.instructions.toArray()) {
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
				VarInsnNode insn = new VarInsnNode(new_opcode,
						methodNode.maxLocals + index);
				methodNode.instructions.insertBefore(instr, insn);
				methodNode.instructions.remove(instr);
			}
		}

		methodNode.maxLocals += syntheticLocalVars.size();
	}

	public static void addExceptionHandlerFrame(MethodNode methodNode,
			InsnList instructions) {
		int max = methodNode.maxLocals;

		instructions.insertBefore(instructions.getFirst(), new IntInsnNode(
				Opcodes.ASTORE, max));
		instructions.add(new IntInsnNode(Opcodes.ALOAD, max));
		instructions.add(new InsnNode(Opcodes.ATHROW));

		methodNode.maxLocals++;
	}

	public static LabelNode getStartLabel(MethodNode methodNode,
			AbstractInsnNode start) {
		if (start instanceof LabelNode) {
			return (LabelNode) start;
		}

		AbstractInsnNode previous = start.getPrevious();

		if (previous != null && previous instanceof LabelNode) {
			return (LabelNode) previous;
		}

		LabelNode label = new LabelNode();
		methodNode.instructions.insertBefore(start, label);
		return label;
	}

	public static LabelNode getEndLabel(MethodNode methodNode,
			AbstractInsnNode end) {
		if (InsnListHelper.isConditionalBranch(end)) {

		}

		return null;
	}

	// TODO support for AfterReturning and AfterThrowing
	// TODO support for static information weaving
	public static void instrument(MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars) {
		// Sort the snippets based on their order
		Map<MarkedRegion, List<AbstractInsnNode>> weaving_ends_map;
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		//
		weaving_ends_map = new HashMap<MarkedRegion, List<AbstractInsnNode>>();

		// Prepare for weaving
		for (Snippet snippet : array) {
			for (MarkedRegion region : snippetMarkings.get(snippet)) {
				weaving_ends_map.put(region, fixRegion(methodNode, region));
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
					fixLocalIndex(methodNode, newlst);
					methodNode.instructions.insertBefore(region.getStart(),
							newlst);
				}
			} else if (snippet.getAnnotationClass()
					.equals(AfterReturning.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : weaving_ends_map.get(region)) {
						InsnList newlst = InsnListHelper.cloneList(ilst);
						fixLocalIndex(methodNode, newlst);
						methodNode.instructions.insert(exit, newlst);
					}
				}
			} else if (snippet.getAnnotationClass().equals(AfterThrowing.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.getEnds()) {
						InsnList newlst = InsnListHelper.cloneList(ilst);
						fixLocalIndex(methodNode, newlst);
						addExceptionHandlerFrame(methodNode, newlst);
						methodNode.instructions.insert(exit, newlst);
					}
				}
			} else if (snippet.getAnnotationClass().equals(After.class)) {

			}
		}

		static2Local(methodNode, syntheticLocalVars);
	}
}
