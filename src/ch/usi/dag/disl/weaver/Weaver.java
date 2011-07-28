package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import ch.usi.dag.disl.snippet.SnippetCode;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.util.AsmHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// Search for an instruction sequence that match the pattern
	// of the pseudo variables.
	public static void fixPseudoVar(Snippet snippet, MarkedRegion region,
			InsnList src, StaticInfo staticInfoHolder) {

		for (AbstractInsnNode instr : src.toArray()) {

			AbstractInsnNode previous = instr.getPrevious();
			// pseudo var are represented by a static function call with
			// a null as its parameter.
			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL || previous == null
					|| previous.getOpcode() != Opcodes.ALOAD) {
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

		methodNode.maxLocals = max + 1;
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

				InsnList newlst = AsmHelper
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

	// Add an exception handler frame to a instruction list.
	// That is, an astore <n> before the list, then an aload <n>
	// and an athrow after the list.
	public static void addExceptionHandlerFrame(MethodNode methodNode,
			InsnList instructions) {
		instructions.insertBefore(instructions.getFirst(), new IntInsnNode(
				Opcodes.ASTORE, methodNode.maxLocals));
		instructions.add(new IntInsnNode(Opcodes.ALOAD, methodNode.maxLocals));
		instructions.add(new InsnNode(Opcodes.ATHROW));

		methodNode.maxLocals++;
	}

	// Return a predecessor label of the input 'start'.
	public static LabelNode getStartLabel(MethodNode methodNode,
			AbstractInsnNode start) {
		// Return start if it is a label.
		if (start instanceof LabelNode) {
			return (LabelNode) start;
		}

		// Or Return start.previous if the previous node of
		// start is a label.
		AbstractInsnNode previous = start.getPrevious();

		if (previous != null && previous instanceof LabelNode) {
			return (LabelNode) previous;
		}

		// Otherwise, create a label before start and return it.
		LabelNode label = AsmHelper.createLabel();

		methodNode.instructions.insertBefore(start, label);
		return label;
	}

	// Return a successor label of weaving location corresponding to
	// the input 'end'.
	public static LabelNode getEndLabel(MethodNode methodNode,
			AbstractInsnNode end,
			Map<AbstractInsnNode, AbstractInsnNode> ends_after_athrow) {
		AbstractInsnNode instr = ends_after_athrow.get(end);

		// Skip branch instructions, label nodes and line number node.
		while (instr.getOpcode() == -1 || AsmHelper.isBranch(instr)) {
			instr = instr.getPrevious();
		}

		// For those instruction that might fall through, there should
		// be a 'GOTO' instruction to separate from the exception handler.
		if (AsmHelper.isConditionalBranch(instr)
				|| !AsmHelper.isBranch(instr)) {

			LabelNode branch = AsmHelper.createLabel();
			methodNode.instructions.insert(instr, branch);

			JumpInsnNode jump = new JumpInsnNode(Opcodes.GOTO, branch);
			methodNode.instructions.insert(instr, jump);
			ends_after_athrow.put(end, instr);

			instr = jump;
		}

		// Create a label just after the 'GOTO' instruction.
		LabelNode label = AsmHelper.createLabel();
		methodNode.instructions.insert(instr, label);
		return label;
	}

	// Sort the try-catch blocks of the method according to the
	// length of each block.
	public static void sortTryCatchBlocks(MethodNode method) {
		TryCatchBlockSorter sorter = new TryCatchBlockSorter(null,
				method.access, method.name, method.desc, method.signature, null);
		sorter.instructions = method.instructions;
		sorter.tryCatchBlocks = method.tryCatchBlocks;
		sorter.visitEnd();
	}

	// TODO dynamic analysis
	// TODO respect initialization type in synthetic local variable
	// TODO try block weaving
	public static void instrument(MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars,
			StaticInfo staticInfoHolder, boolean usesDynamicAnalysis) {
		// Sort the snippets based on their order
		Map<AbstractInsnNode, AbstractInsnNode> weaving_loc_normal;
		Map<AbstractInsnNode, AbstractInsnNode> weaving_loc_athrow;
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		// Prepare for weaving
		weaving_loc_normal = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		weaving_loc_athrow = new HashMap<AbstractInsnNode, AbstractInsnNode>();

		for (Snippet snippet : array) {

			for (MarkedRegion region : snippetMarkings.get(snippet)) {
				// initialize of start
				AbstractInsnNode start = region.getStart();

				if (weaving_loc_normal.get(start) == null) {
					weaving_loc_normal.put(start, start);
				}

				for (AbstractInsnNode end : region.getEnds()) {
					if (AsmHelper.isBranch(end)) {

						if (start == end) {
							// Contains only one instruction
							LabelNode labelNode = AsmHelper.createLabel();
							methodNode.instructions.insertBefore(start,
									labelNode);
							weaving_loc_normal.put(start, labelNode);
							weaving_loc_normal.put(end, labelNode);
						} else {
							// Skip branch instructions and ASM label nodes.
							AbstractInsnNode instr = end.getPrevious();

							while (instr != start
									&& (instr.getOpcode() == -1 || AsmHelper
											.isBranch(instr))) {
								instr = instr.getPrevious();
							}

							weaving_loc_normal.put(end, instr);
						}
					} else {
						// this one is not a branch instruction, which
						// means instruction list can be inserted after
						// this instruction.
						weaving_loc_normal.put(end, end);
					}

					// initial value, might be changed during the weaving
					weaving_loc_athrow.put(end, end);
				}
			}
		}

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			SnippetCode code = snippet.getCode();

			// skip snippet with empty code
			if (snippet.getCode() == null) {
				continue;
			}

			// Instrument
			// For @Before, instrument the snippet just before the
			// entrance of a region.
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {

					SnippetCode clone = code.clone();
					InsnList newlst = clone.getInstructions();

					fixPseudoVar(snippet, region, newlst, staticInfoHolder);
					fixLocalIndex(methodNode, newlst);
					methodNode.instructions.insertBefore(
							weaving_loc_normal.get(region.getStart()), newlst);
					methodNode.tryCatchBlocks.addAll(clone.getTryCatchBlocks());
				}
			}

			// For normal after(after returning), instrument the snippet
			// after each adjusted exit of a region.
			if (snippet.getAnnotationClass().equals(AfterReturning.class)
					|| snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {

					for (AbstractInsnNode exit : region.getEnds()) {

						SnippetCode clone = code.clone();
						InsnList newlst = clone.getInstructions();
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						fixLocalIndex(methodNode, newlst);
						methodNode.instructions.insert(
								weaving_loc_normal.get(exit), newlst);
						methodNode.tryCatchBlocks.addAll(clone
								.getTryCatchBlocks());
					}
				}
			}

			// For exceptional after(after throwing), wrap the region with
			// a try-finally clause. And append the snippet as an exception
			// handler.
			if (snippet.getAnnotationClass().equals(AfterThrowing.class)
					|| snippet.getAnnotationClass().equals(After.class)) {

				for (MarkedRegion region : regions) {

					for (AbstractInsnNode exit : region.getEnds()) {

						SnippetCode clone = code.clone();
						InsnList newlst = clone.getInstructions();
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						fixLocalIndex(methodNode, newlst);

						// Create a try-catch clause
						LabelNode startLabel = getStartLabel(methodNode,
								region.getStart());
						LabelNode endLabel = getEndLabel(methodNode, exit,
								weaving_loc_athrow);

						methodNode.visitTryCatchBlock(startLabel.getLabel(),
								endLabel.getLabel(), endLabel.getLabel(), null);
						addExceptionHandlerFrame(methodNode, newlst);
						methodNode.instructions.insert(endLabel, newlst);
						methodNode.tryCatchBlocks.addAll(clone
								.getTryCatchBlocks());
					}
				}
			}
		}
		
		sortTryCatchBlocks(methodNode);
		// TODO ProcessorHack uncomment
		// static2Local(methodNode, syntheticLocalVars);
	}
}
