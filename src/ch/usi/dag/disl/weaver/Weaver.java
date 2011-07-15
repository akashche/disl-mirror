package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	private static LabelNode createLabel() {
		Label label = new Label();
		LabelNode labelNode = new LabelNode(label);
		label.info = labelNode;
		return labelNode;
	}

	public static List<AbstractInsnNode> fixRegion(MethodNode methodNode,
			MarkedRegion region) {
		List<AbstractInsnNode> weaving_ends = new LinkedList<AbstractInsnNode>();
		AbstractInsnNode start = region.getStart();

		for (AbstractInsnNode end : region.getEnds()) {
			if (InsnListHelper.isBranch(end)) {
				if (start == end) {
					LabelNode labelNode = createLabel();
					methodNode.instructions.insertBefore(start, labelNode);
					region.setStart(labelNode);
					weaving_ends.add(end.getPrevious());
				} else {
					AbstractInsnNode instr = end.getPrevious();

					while (instr != start
							&& (instr.getOpcode() == -1 || InsnListHelper
									.isBranch(instr))) {
						instr = instr.getPrevious();
					}

					weaving_ends.add(instr);
				}
			}
		}

		return weaving_ends;
	}

	public static void fixPseudoVar(Snippet snippet, MarkedRegion region,
			InsnList src, StaticInfo staticInfoHolder) {

		for (AbstractInsnNode instr : src.toArray()) {

			AbstractInsnNode previous = instr.getPrevious();
			// pseudo var are represented by a static function call with
			// a null as its parameter.
			if (instr.getOpcode() != Opcodes.INVOKESTATIC || previous == null
					|| previous.getOpcode() != Opcodes.ACONST_NULL) {
				continue;
			}

			MethodInsnNode invocation = (MethodInsnNode) instr;
			Object const_var = staticInfoHolder.getSI(snippet, region,
					invocation.owner, invocation.name);

			if (const_var != null) {
				// Insert a ldc instruction and remove the pseudo ones.
				src.insert(instr, new LdcInsnNode(const_var));
				src.remove(previous);
				src.remove(instr);
			}
		}
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

		LabelNode label = createLabel();

		methodNode.instructions.insertBefore(start, label);
		return label;
	}

	public static LabelNode getEndLabel(MethodNode methodNode,
			AbstractInsnNode end,
			Map<AbstractInsnNode, AbstractInsnNode> ends_after_athrow) {
		AbstractInsnNode instr = ends_after_athrow.get(end);

		while (instr.getOpcode() == -1 || InsnListHelper.isBranch(instr)) {
			instr = instr.getPrevious();
		}

		if (InsnListHelper.isConditionalBranch(instr)
				|| !InsnListHelper.isBranch(instr)) {

			LabelNode branch = createLabel();
			methodNode.instructions.insert(instr, branch);

			JumpInsnNode jump = new JumpInsnNode(Opcodes.GOTO, branch);
			methodNode.instructions.insert(instr, jump);
			ends_after_athrow.put(end, instr);

			instr = jump;
		}

		LabelNode label = createLabel();
		methodNode.instructions.insert(instr, label);
		return label;
	}

	public static void sortTryCatchBlocks(MethodNode method) {
		TryCatchBlockSorter sorter = new TryCatchBlockSorter(null,
				method.access, method.name, method.desc, method.signature, null);
		sorter.instructions = method.instructions;
		sorter.tryCatchBlocks = method.tryCatchBlocks;
		sorter.visitEnd();
	}

	// TODO support for AfterReturning and AfterThrowing
	// TODO support for static information weaving
	public static void instrument(MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars,
			StaticInfo staticInfoHolder) {
		// Sort the snippets based on their order
		Map<MarkedRegion, List<AbstractInsnNode>> ends_after_return;
		Map<AbstractInsnNode, AbstractInsnNode> ends_after_athrow;
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		// Prepare for weaving
		ends_after_return = new HashMap<MarkedRegion, List<AbstractInsnNode>>();
		ends_after_athrow = new HashMap<AbstractInsnNode, AbstractInsnNode>();

		for (Snippet snippet : array) {

			for (MarkedRegion region : snippetMarkings.get(snippet)) {

				List<AbstractInsnNode> weaving_ends = fixRegion(methodNode,
						region);
				ends_after_return.put(region, weaving_ends);

				for (AbstractInsnNode end : region.getEnds()) {
					ends_after_athrow.put(end, end);
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
					fixPseudoVar(snippet, region, newlst, staticInfoHolder);
					fixLocalIndex(methodNode, newlst);
					methodNode.instructions.insertBefore(region.getStart(),
							newlst);
				}
			}

			if (snippet.getAnnotationClass().equals(AfterReturning.class)
					|| snippet.getAnnotationClass().equals(After.class)) {

				for (MarkedRegion region : regions) {

					for (AbstractInsnNode exit : ends_after_return.get(region)) {

						InsnList newlst = InsnListHelper.cloneList(ilst);
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						fixLocalIndex(methodNode, newlst);
						methodNode.instructions.insert(exit, newlst);
					}
				}
			}

			if (snippet.getAnnotationClass().equals(AfterThrowing.class)
					|| snippet.getAnnotationClass().equals(After.class)) {

				for (MarkedRegion region : regions) {

					for (AbstractInsnNode exit : region.getEnds()) {

						InsnList newlst = InsnListHelper.cloneList(ilst);
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						fixLocalIndex(methodNode, newlst);

						LabelNode startLabel = getStartLabel(methodNode,
								region.getStart());
						LabelNode endLabel = getEndLabel(methodNode, exit,
								ends_after_athrow);

						methodNode.visitTryCatchBlock(startLabel.getLabel(),
								endLabel.getLabel(), endLabel.getLabel(), null);
						addExceptionHandlerFrame(methodNode, newlst);
						methodNode.instructions.insert(endLabel, newlst);
					}
				}
			}
		}

		sortTryCatchBlocks(methodNode);
		// TODO ProcessorHack uncomment
		// static2Local(methodNode, syntheticLocalVars);
	}
}
