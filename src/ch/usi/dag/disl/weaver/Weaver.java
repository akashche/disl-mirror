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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.exception.ASMException;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetCode;
import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.stack.StackUtil;

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
			
			if (staticInfoHolder.contains(snippet, region, invocation.owner,
					invocation.name)) {

				Object const_var = staticInfoHolder.get(snippet, region,
						invocation.owner, invocation.name);

				if (const_var != null) {
					// Insert a ldc instruction
					src.insert(instr, new LdcInsnNode(const_var));
				} else {
					// push null onto the stack
					src.insert(instr, new InsnNode(Opcodes.ACONST_NULL));
				}

				// remove the pseudo instructions
				src.remove(previous);
				src.remove(instr);
			}
		}
	}

	public static void fixDynamicInfo(Snippet snippet, MarkedRegion region,
			Frame<SourceValue> frame, MethodNode method)
			throws ASMException {
		InsnList src = method.instructions;

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			AbstractInsnNode prev = instr.getPrevious();
			AbstractInsnNode next = instr.getNext();

			int operand = AsmHelper.getIConstOperand(prev.getPrevious());
			Type t = AsmHelper.getClassType(prev);

			if (invoke.name.equals("getStackValue")) {
				int sopcode = t.getOpcode(Opcodes.ISTORE);
				int lopcode = t.getOpcode(Opcodes.ILOAD);
				SourceValue source = StackUtil.getSource(frame, operand);

				for (AbstractInsnNode itr : source.insns) {
					method.instructions.insert(itr, new VarInsnNode(sopcode,
							method.maxLocals));
					method.instructions.insert(itr, new InsnNode(
							source.size == 2 ? Opcodes.DUP2 : Opcodes.DUP));
				}

				src.insert(instr, new VarInsnNode(lopcode, method.maxLocals));
				method.maxLocals += source.size;
			} else if (invoke.name.equals("getMethodArgumentValue")) {

				method.instructions.insert(
						instr,
						new VarInsnNode(t.getOpcode(Opcodes.ILOAD), AsmHelper
								.getParameterIndex(method, operand)));
			} else if (invoke.name.equals("getLocalVariableValue")) {

				method.instructions.insert(instr,
						new VarInsnNode(t.getOpcode(Opcodes.ILOAD), operand));
			}

			src.remove(instr);

			prev = AsmHelper.remove(src, prev, false);
			prev = AsmHelper.remove(src, prev, false);
			prev = AsmHelper.removeIf(src, prev, Opcodes.ALOAD, false);

			next = AsmHelper.removeIf(src, next, Opcodes.CHECKCAST, true);
			next = AsmHelper.removeIf(src, next, Opcodes.INVOKEVIRTUAL, true);
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

				switch (varInstr.getOpcode()) {
				case Opcodes.LLOAD:
				case Opcodes.DLOAD:
				case Opcodes.LSTORE:
				case Opcodes.DSTORE:

					if ((varInstr.var + 1) > max) {
						max = varInstr.var + 1;
					}

					break;

				default:
					if (varInstr.var > max) {
						max = varInstr.var;
					}

					break;
				}
			}
		}

		methodNode.maxLocals = max + 1;
	}
	
	public static void threadlocal_translate(MethodNode methodNode,
			List<ThreadLocalVar> threadLocalVars) {

		InsnList instructions = methodNode.instructions;

		for (AbstractInsnNode instr : instructions.toArray()) {

			int opcode = instr.getOpcode();

			// Only field instructions will be transformed.
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {

				FieldInsnNode field_instr = (FieldInsnNode) instr;
				String id = field_instr.owner + SyntheticLocalVar.NAME_DELIM
						+ field_instr.name;

				// Check if it is a thread local

				for (ThreadLocalVar var : threadLocalVars) {

					if (!id.equals(var.getID())) {
						continue;
					}

					int new_opcode = (opcode == Opcodes.GETSTATIC) ? 
							Opcodes.GETFIELD : Opcodes.PUTFIELD;

					instructions.insertBefore(instr, new MethodInsnNode(
							Opcodes.INVOKESTATIC, "java/lang/Thread",
							"currentThread", "()Ljava/lang/Thread;"));
					instructions.insertBefore(instr, new FieldInsnNode(
							new_opcode, "java/lang/Thread", field_instr.name,
							field_instr.desc));
					instructions.remove(instr);
				}
			}
		}
	}

	// Transform static fields to synthetic local
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void static2Local(MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocalVars) {

		InsnList instructions = methodNode.instructions;

		// Extract the 'id' field in each synthetic local
		List<String> id_set = new LinkedList<String>();
		AbstractInsnNode first = instructions.getFirst();

		// Initialization
		for (SyntheticLocalVar var : syntheticLocalVars) {

			if (var.getInitASMCode() != null) {

				InsnList newlst = AsmHelper.cloneInsnList(var.getInitASMCode());
				instructions.insertBefore(first, newlst);
			}

			id_set.add(var.getID());
		}

		// Scan for FIELD instructions and replace with local load/store.
		for (AbstractInsnNode instr : instructions.toArray()) {
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
				instructions.insertBefore(instr, insn);
				instructions.remove(instr);
			}
		}

		methodNode.maxLocals += syntheticLocalVars.size();
	}

	// Add an exception handler frame to a instruction list.
	// That is, an astore <n> before the list, then an aload <n>
	// and an athrow after the list.
	public static void addExceptionHandlerFrame(MethodNode methodNode,
			InsnList instructions) {
		instructions.insertBefore(instructions.getFirst(), new VarInsnNode(
				Opcodes.ASTORE, methodNode.maxLocals));
		instructions.add(new VarInsnNode(Opcodes.ALOAD, methodNode.maxLocals));
		instructions.add(new InsnNode(Opcodes.ATHROW));

		methodNode.maxLocals++;
	}

	// Return a predecessor label of the input 'start'.
	public static LabelNode getStartLabel(MethodNode methodNode,
			AbstractInsnNode start) {
		LabelNode label = new LabelNode();

		// Return start if it is a label.
		if (start instanceof LabelNode) {
			methodNode.instructions.insert(start, label);
		} else {
			methodNode.instructions.insertBefore(start, label);
		}

		return label;
	}

	// Return a successor label of weaving location corresponding to
	// the input 'end'.
	public static LabelNode getEndLabel(MethodNode methodNode,
			AbstractInsnNode instr) {
		// For those instruction that might fall through, there should
		// be a 'GOTO' instruction to separate from the exception handler.
		if (AsmHelper.isConditionalBranch(instr) || !AsmHelper.isBranch(instr)) {

			LabelNode branch = new LabelNode();
			methodNode.instructions.insert(instr, branch);

			JumpInsnNode jump = new JumpInsnNode(Opcodes.GOTO, branch);
			methodNode.instructions.insert(instr, jump);
			instr = jump;
		}

		// Create a label just after the 'GOTO' instruction.
		LabelNode label = new LabelNode();
		methodNode.instructions.insert(instr, label);
		return label;
	}

	public static void initWeavingEnds(MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			Map<AbstractInsnNode, AbstractInsnNode> weaving_start,
			Map<AbstractInsnNode, AbstractInsnNode> weaving_end,
			Map<AbstractInsnNode, AbstractInsnNode> weaving_athrow,
			Map<AbstractInsnNode, Integer> stack_start,
			Map<AbstractInsnNode, Integer> stack_end, ArrayList<Snippet> array) {

		InsnList instructions = methodNode.instructions;
		List<LabelNode> instrumented = new LinkedList<LabelNode>();

		List<LabelNode> tcb_ends = new LinkedList<LabelNode>();

		for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
			tcb_ends.add(tcb.end);
		}

		for (Snippet snippet : array) {

			for (MarkedRegion region : snippetMarkings.get(snippet)) {
				// initialize of start
				AbstractInsnNode start = region.getStart();

				if (weaving_start.get(start) == null) {
					weaving_start.put(start, start);
				}

				for (AbstractInsnNode end : region.getEnds()) {

					AbstractInsnNode wend = AsmHelper.skipLabels(end, false);

					if (AsmHelper.isBranch(wend)) {

						AbstractInsnNode prev = wend.getPrevious();

						if (start == wend) {
							// Contains only one instruction
							if (!(prev != null && prev instanceof LabelNode && 
									instrumented.contains(prev))) {

								LabelNode labelNode = new LabelNode();
								instrumented.add(labelNode);
								instructions.insertBefore(start, labelNode);
								prev = labelNode;
							}

							weaving_start.put(start, prev);
							weaving_end.put(end, prev);
							weaving_athrow.put(end, prev);
						} else {
							weaving_end.put(end, prev);

							while (tcb_ends.contains(prev)) {

								if (prev == start) {

									LabelNode labelNode = new LabelNode();
									instrumented.add(labelNode);
									instructions.insert(start, labelNode);
									prev = labelNode;
									break;
								}

								prev = prev.getPrevious();
							}

							weaving_athrow.put(end, prev);
						}
					} else {
						// this one is not a branch instruction, which
						// means instruction list can be inserted after
						// this instruction.
						weaving_end.put(end, end);
						weaving_athrow.put(end, end);
					}
				}
			}
		}

		for (Snippet snippet : array) {

			for (MarkedRegion region : snippetMarkings.get(snippet)) {

				AbstractInsnNode start = region.getStart();

				if (stack_start.get(start) == null) {
					stack_start.put(start, instructions.indexOf(start));
				}

				for (AbstractInsnNode end : region.getEnds()) {

					if (AsmHelper.isBranch(end)) {
						stack_end.put(end, instructions.indexOf(end));
					} else {
						stack_end.put(end, instructions.indexOf(end.getNext()));
					}
				}
			}
		}
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

	// TODO test SnippetCode.containsHandledException()
	// TODO respect BEST_EFFORT initialization type in synthetic local variable
	public static void instrument(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars,
			LinkedList<ThreadLocalVar> linkedList, StaticInfo staticInfoHolder,
			boolean usesDynamicAnalysis) throws ASMException {
		// Sort the snippets based on their order
		Map<AbstractInsnNode, AbstractInsnNode> weaving_start;
		Map<AbstractInsnNode, AbstractInsnNode> weaving_end;
		Map<AbstractInsnNode, AbstractInsnNode> weaving_athrow;

		Map<AbstractInsnNode, Integer> stack_start;
		Map<AbstractInsnNode, Integer> stack_end;

		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		// Prepare for weaving
		weaving_start = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		weaving_end = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		weaving_athrow = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		stack_start = new HashMap<AbstractInsnNode, Integer>();
		stack_end = new HashMap<AbstractInsnNode, Integer>();

		initWeavingEnds(methodNode, snippetMarkings, weaving_start,
				weaving_end, weaving_athrow, stack_start, stack_end, array);

		Analyzer<BasicValue> basicAnalyzer = StackUtil.getBasicAnalyzer();

		try {
			basicAnalyzer.analyze(classNode.name, methodNode);
		} catch (AnalyzerException e) {
			throw new DiSLFatalException("Cause by AnalyzerException : \n"
					+ e.getMessage());
		}

		Frame<BasicValue>[] basicFrames = basicAnalyzer.getFrames();

		Analyzer<SourceValue> sourceAnalyzer = null;
		Frame<SourceValue>[] sourceFrames = null;

		if (usesDynamicAnalysis) {
			sourceAnalyzer = StackUtil.getSourceAnalyzer();

			try {
				sourceAnalyzer.analyze(classNode.name, methodNode);
			} catch (AnalyzerException e) {
				throw new DiSLFatalException("Cause by AnalyzerException : \n"
						+ e.getMessage());
			}

			sourceFrames = sourceAnalyzer.getFrames();
		}

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			SnippetCode code = snippet.getCode();

			// skip snippet with empty code
			if (code == null) {
				continue;
			}

			// Instrument
			// For @Before, instrument the snippet just before the
			// entrance of a region.
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {

					AbstractInsnNode loc = weaving_start.get(region.getStart());
					int index = stack_start.get(region.getStart());

					if (code.containsHandledException()) {
						
						InsnList popAll = StackUtil.enter(basicFrames[index],
								methodNode.maxLocals);
						InsnList pushAll = StackUtil.exit(basicFrames[index],
								methodNode.maxLocals);
						methodNode.maxLocals += StackUtil
								.getOffset(basicFrames[index]);
						
						AbstractInsnNode temp = pushAll.getFirst();

						methodNode.instructions.insertBefore(loc, popAll);
						methodNode.instructions.insertBefore(loc, pushAll);
						
						loc = temp;
					}

					SnippetCode clone = code.clone();
					InsnList newlst = clone.getInstructions();

					fixPseudoVar(snippet, region, newlst, staticInfoHolder);
					fixLocalIndex(methodNode, newlst);

					methodNode.instructions.insertBefore(loc, newlst);
					methodNode.tryCatchBlocks.addAll(clone.getTryCatchBlocks());

					if (usesDynamicAnalysis) {
						fixDynamicInfo(snippet, region, sourceFrames[index],
								methodNode);
					}
				}
			}

			// For normal after(after returning), instrument the snippet
			// after each adjusted exit of a region.
			if (snippet.getAnnotationClass().equals(AfterReturning.class)
					|| snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {

					for (AbstractInsnNode exit : region.getEnds()) {

						AbstractInsnNode loc = weaving_end.get(exit);

						int index = stack_end.get(exit);
						
						if (code.containsHandledException()) {

							InsnList popAll = StackUtil.enter(
									basicFrames[index], methodNode.maxLocals);
							InsnList pushAll = StackUtil.exit(
									basicFrames[index], methodNode.maxLocals);
							methodNode.maxLocals += StackUtil
									.getOffset(basicFrames[index]);

							AbstractInsnNode temp = popAll.getLast();

							methodNode.instructions.insert(loc, pushAll);
							methodNode.instructions.insert(loc, popAll);

							loc = temp;
						}

						SnippetCode clone = code.clone();
						InsnList newlst = clone.getInstructions();
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						fixLocalIndex(methodNode, newlst);
						methodNode.instructions.insert(loc, newlst);
						methodNode.tryCatchBlocks.addAll(clone
								.getTryCatchBlocks());

						if (usesDynamicAnalysis) {
							fixDynamicInfo(snippet, region,
									sourceFrames[index], methodNode);
						}
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
								weaving_start.get(region.getStart()));
						LabelNode endLabel = getEndLabel(methodNode,
								weaving_athrow.get(exit));

						methodNode.tryCatchBlocks.add(new TryCatchBlockNode(
								startLabel, endLabel, endLabel, null));
						addExceptionHandlerFrame(methodNode, newlst);
						methodNode.instructions.insert(endLabel, newlst);
						methodNode.tryCatchBlocks.addAll(clone
								.getTryCatchBlocks());

						if (usesDynamicAnalysis) {
							fixDynamicInfo(snippet, region,
									sourceFrames[stack_end.get(exit)],
									methodNode);
						}
					}
				}
			}
		}

		sortTryCatchBlocks(methodNode);
		// TODO ProcessorHack uncomment
		// static2Local(methodNode, syntheticLocalVars);
	}

}
