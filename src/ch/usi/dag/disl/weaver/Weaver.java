package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.AfterThrowing;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.SnippetCode;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.DynamicInfoException;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.processor.generator.ProcInstance;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.stack.StackUtil;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	/**
	 * Checks if dynamic analysis methods contains only constants
	 */
	private static void passesConstsToDynamicAnalysis(Snippet snippet,
			InsnList instructions) throws DynamicInfoException {

		for (AbstractInsnNode instr : instructions.toArray()) {

			// it is invocation...
			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			// ... of dynamic analysis
			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			AbstractInsnNode secondOperand = instr.getPrevious();
			AbstractInsnNode firstOperand = secondOperand.getPrevious();

			// first operand test
			switch (firstOperand.getOpcode()) {
			case Opcodes.ICONST_M1:
			case Opcodes.ICONST_0:
			case Opcodes.ICONST_1:
			case Opcodes.ICONST_2:
			case Opcodes.ICONST_3:
			case Opcodes.ICONST_4:
			case Opcodes.ICONST_5:
			case Opcodes.BIPUSH:
				break;

			default:
				throw new DynamicInfoException("In advice "
						+ snippet.getOriginClassName() + "."
						+ snippet.getOriginMethodName()
						+ " - pass the first (pos)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: getStackValue(1, int.class)");
			}

			// second operand test
			if (AsmHelper.getClassType(secondOperand) == null) {
				throw new DynamicInfoException("In advice "
						+ snippet.getOriginClassName() + "."
						+ snippet.getOriginMethodName()
						+ " - pass the second (type)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: getStackValue(1, int.class)");
			}
		}
	}
	
	// initialize weaving locations
	private static void initWeavingLocations(MethodNode methodNode,
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
				// initialize weaving start
				AbstractInsnNode start = region.getStart();
				AbstractInsnNode wstart = AsmHelper.skipVirualInsns(start, true);

				if (weaving_start.get(start) == null) {
					weaving_start.put(start, start);
				}

				for (AbstractInsnNode end : region.getEnds()) {

					AbstractInsnNode wend = AsmHelper.skipVirualInsns(end, false);

					if (AsmHelper.isBranch(wend)) {

						AbstractInsnNode prev = wend.getPrevious();

						if (wstart == wend) {
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

		// initialize stack_start and stack_end
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
					src.insert(instr, AsmHelper.loadConst(const_var));
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

	// Search for an instruction sequence that stands for a request for dynamic
	// information, and replace them with a load instruction.
	// NOTE that if the user requests for the stack value, some store 
	// instructions will be inserted to the target method, and new local slot
	// will be used for storing this.
	public static void fixDynamicInfo(Snippet snippet, MarkedRegion region,
			Frame<SourceValue> frame, MethodNode method, InsnList src) {

		int max = AsmHelper.calcMaxLocal(src);

		for (AbstractInsnNode instr : src.toArray()) {
			// pseudo function call
			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			// parsing:
			//		aload dynamic_info
			//		iconst
			//		ldc class
			//		invoke  (current instruction)
			//		[checkcast]
			//		[invoke]
			AbstractInsnNode prev = instr.getPrevious();
			AbstractInsnNode next = instr.getNext();

			int operand = AsmHelper.getIConstOperand(prev.getPrevious());
			Type t = AsmHelper.getClassType(prev);

			if (invoke.name.equals("getStackValue")) {
				int sopcode = t.getOpcode(Opcodes.ISTORE);
				int lopcode = t.getOpcode(Opcodes.ILOAD);
				
				// store the stack value without changing the semantic
				int size = dupStack(frame, method, operand, sopcode,
						method.maxLocals + max);
				// load the stack value
				src.insert(instr, new VarInsnNode(lopcode, max));
				max += size;
			} 
			// TRICK: the following two situation will generate a VarInsnNode
			//        with a negative local slot. And it will be updated in 
			//        method fixLocalIndex
			else if (invoke.name.equals("getMethodArgumentValue")) {
				
				src.insert(instr, new VarInsnNode(t.getOpcode(Opcodes.ILOAD),
						AsmHelper.getInternalParamIndex(method, operand)
								- method.maxLocals));
			} else if (invoke.name.equals("getLocalVariableValue")) {

				src.insert(instr, new VarInsnNode(t.getOpcode(Opcodes.ILOAD),
						operand - method.maxLocals));
			}

			// remove invoke
			src.remove(instr);

			// remove aload, iconst, ldc
			for (int i = 0; i < 3; i++) {
				instr = prev;
				prev = prev.getPrevious();
				src.remove(instr);
			}

			// remove checkcast, invoke
			if (next.getOpcode() == Opcodes.CHECKCAST) {
				instr = next;
				next = next.getNext();
				src.remove(instr);

				if (next.getOpcode() == Opcodes.INVOKEVIRTUAL) {
					src.remove(next);
				}
			}
		}
	}

	// find out where a stack operand is pushed onto stack, and duplicate the
	// operand and store into a local slot.
	private static int dupStack(Frame<SourceValue> frame,
			MethodNode method, int operand, int sopcode, int slot) {
		SourceValue source = StackUtil.getSource(frame, operand);

		for (AbstractInsnNode itr : source.insns) {

			// if the instruction duplicates two-size operand(s), weaver should
			// be careful that the operand might be either 2 one-size operands,
			// or 1 two-size operand.
			switch (itr.getOpcode()) {

			case Opcodes.DUP2:
				if (source.size != 1) {
					break;
				}

				dupStack(frame, method, operand + 2, sopcode, slot);
				continue;

			case Opcodes.DUP2_X1:
				if (source.size != 1) {
					break;
				}

				dupStack(frame, method, operand + 3, sopcode, slot);
				continue;

			case Opcodes.DUP2_X2:
				if (source.size != 1) {
					break;
				}

				SourceValue x2 = StackUtil.getSource(frame, operand + 2);
				dupStack(frame, method, operand + (4 - x2.size), sopcode, slot);
				continue;

			default:
				break;
			}
			
			// insert 'dup' instruction and then store to a local slot
			method.instructions.insert(itr, new VarInsnNode(sopcode, slot));
			method.instructions.insert(itr, new InsnNode(
					source.size == 2 ? Opcodes.DUP2 : Opcodes.DUP));
		}
		
		return source.size;
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

					if ((varInstr.var + 2) > max) {
						max = varInstr.var + 2;
					}

					break;

				default:
					if ((varInstr.var + 1) > max) {
						max = varInstr.var + 1;
					}

					break;
				}
			} else if (instr instanceof IincInsnNode) {

				IincInsnNode iinc = (IincInsnNode) instr;
				iinc.var += methodNode.maxLocals;

				if ((iinc.var + 1) > max) {
					max = iinc.var + 1;
				}
			}
		}

		methodNode.maxLocals = max;
	}
	
	// combine processors into an instruction list
	// NOTE that these processors are for the current method
	private static InsnList procInMethod(MethodNode method,
			ProcInstance processor) {
		InsnList ilist = new InsnList();

		for (ProcMethodInstance processorMethod : processor.getMethods()) {

			Code code = processorMethod.getCode().clone();
			InsnList instructions = code.getInstructions();
			Type type = processorMethod.getArgType().getASMType();
			// initialize the parameters of a processor, including argument
			// position, arguments count and argument value
			AbstractInsnNode start = instructions.getFirst();
			instructions.insertBefore(start,
					AsmHelper.getIConstInstr(processorMethod.getArgPos()));
			instructions
					.insertBefore(start, new VarInsnNode(Opcodes.ISTORE, 0));
			
			instructions.insertBefore(start,
					AsmHelper.getIConstInstr(processorMethod.getArgsCount()));
			instructions
					.insertBefore(start, new VarInsnNode(Opcodes.ISTORE, 1));

			VarInsnNode target = new VarInsnNode(
					type.getOpcode(Opcodes.ISTORE), 2);
			instructions.insertBefore(start, target);
			// optional argument: type name
			String typeName = processorMethod.getArgTypeName();
			if (typeName != null) {
				instructions.insertBefore(start, new LdcInsnNode(typeName));
				instructions.insertBefore(start, new VarInsnNode(
						Opcodes.ASTORE, 2 + type.getSize()));
			}

			fixLocalIndex(method, instructions);

			instructions.insertBefore(
					target,
					new VarInsnNode(type.getOpcode(Opcodes.ILOAD), AsmHelper
							.getInternalParamIndex(method,
									processorMethod.getArgPos())));
			
			ilist.add(instructions);
			method.tryCatchBlocks.addAll(code.getTryCatchBlocks());
		}

		return ilist;
	}

	// combine processors into an instruction list
	// NOTE that these processors are for the callee
	private static InsnList procBeforeInvoke(MethodNode method,
			ProcInstance processor, Frame<SourceValue> frame) {
		
		InsnList ilist = new InsnList();

		for (ProcMethodInstance processorMethod : processor.getMethods()) {

			Code code = processorMethod.getCode().clone();
			InsnList instructions = code.getInstructions();
			// initialize the parameters of a processor, including argument
			// position, arguments count and argument value
			int pos = processorMethod.getArgsCount() - 1
					- processorMethod.getArgPos();
			SourceValue source = StackUtil.getSource(frame, pos);

			AbstractInsnNode start = instructions.getFirst();
			instructions.insertBefore(start,
					AsmHelper.getIConstInstr(processorMethod.getArgPos()));
			instructions
					.insertBefore(start, new VarInsnNode(Opcodes.ISTORE, 0));

			instructions.insertBefore(start,
					AsmHelper.getIConstInstr(processorMethod.getArgsCount()));
			instructions
					.insertBefore(start, new VarInsnNode(Opcodes.ISTORE, 1));

			Type type = processorMethod.getArgType().getASMType();
			int sopcode = type.getOpcode(Opcodes.ISTORE);

			for (AbstractInsnNode itr : source.insns) {
				method.instructions.insert(itr, new VarInsnNode(sopcode,
						// TRICK: the value has to be set properly because
						// method code will be not adjusted by fixLocalIndex
						method.maxLocals + 2));
				method.instructions.insert(itr, new InsnNode(
						type.getSize() == 2 ? Opcodes.DUP2 : Opcodes.DUP));
			}
			// optional argument: type name
			String typeName = processorMethod.getArgTypeName();
			if (typeName != null) {
				instructions.insertBefore(start, new LdcInsnNode(typeName));
				instructions.insertBefore(start, new VarInsnNode(
						Opcodes.ASTORE, 2 + type.getSize()));
			}

			fixLocalIndex(method, instructions);
			ilist.add(instructions);
			method.tryCatchBlocks.addAll(code.getTryCatchBlocks());
		}

		return ilist;
	}

	// replace processor-applying pseudo invocation with processors
	public static void weavingProcessor(MethodNode methodNode,
			PIResolver piResolver, Snippet snippet, MarkedRegion region,
			InsnList newlst, Set<Integer> set, AbstractInsnNode[] array,
			Frame<SourceValue> frame) {
				
		for (int index : set) {

			AbstractInsnNode instr = array[index];
			ProcInstance processor = piResolver.get(snippet, region, index);
			
			if (processor != null) {
				if (processor.getProcApplyType() == 
					ProcessorApplyType.BEFORE_INVOCATION) {

					newlst.insert(instr,
							procBeforeInvoke(methodNode, processor, frame));
				} else {

					newlst.insert(instr, procInMethod(methodNode, processor));
				}
			}

			// remove pseudo invocation
			newlst.remove(instr.getPrevious().getPrevious());
			newlst.remove(instr.getPrevious());
			newlst.remove(instr);
		}
	}

	// Transform static fields to synthetic local
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public static void static2Local(MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocalVars) {

		InsnList instructions = methodNode.instructions;

		// Extract the 'id' field in each synthetic local
		AbstractInsnNode first = instructions.getFirst();

		// Initialization
		// TODO implement BESTEFFORT
		for (SyntheticLocalVar var : syntheticLocalVars) {

			if (var.getInitialize() == Initialize.NEVER) {
				continue;
			}

			if (var.getInitASMCode() != null) {

				InsnList newlst = AsmHelper.cloneInsnList(var.getInitASMCode());
				instructions.insertBefore(first, newlst);
			} else {

				Type type = var.getType();

				switch (type.getSort()) {
				case Type.ARRAY:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ACONST_NULL));
					instructions.insertBefore(first, new TypeInsnNode(
							Opcodes.CHECKCAST, type.getDescriptor()));
					break;

				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.INT:
				case Type.SHORT:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ICONST_0));
					break;

				case Type.DOUBLE:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.DCONST_0));
					break;

				case Type.FLOAT:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.FCONST_0));
					break;

				case Type.LONG:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.LCONST_0));
					break;

				case Type.OBJECT:
				default:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ACONST_NULL));
					break;
				}

				instructions.insertBefore(first,
						new FieldInsnNode(Opcodes.PUTSTATIC, var.getOwner(),
								var.getName(), type.getDescriptor()));
			}
		}

		// Scan for FIELD instructions and replace with local load/store.
		for (AbstractInsnNode instr : instructions.toArray()) {
			int opcode = instr.getOpcode();

			// Only field instructions will be transformed.
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {

				FieldInsnNode field_instr = (FieldInsnNode) instr;
				String id = field_instr.owner + SyntheticLocalVar.NAME_DELIM
						+ field_instr.name;

				int index = 0, count = 0;

				for (SyntheticLocalVar var : syntheticLocalVars) {

					if (id.equals(var.getID())) {
						break;
					}

					index += var.getType().getSize();
					count++;
				}

				if (count == syntheticLocalVars.size()) {
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
	private static LabelNode getStartLabel(MethodNode methodNode,
			AbstractInsnNode start) {
		LabelNode label = new LabelNode();
		methodNode.instructions.insertBefore(start, label);
		return label;
	}

	// Return a successor label of weaving location corresponding to
	// the input 'end'.
	private static LabelNode getEndLabel(MethodNode methodNode,
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


	// generate a try catch block node given the scope of the handler
	public static TryCatchBlockNode getTryCatchBlock(MethodNode methodNode,
			AbstractInsnNode start, AbstractInsnNode end) {

		InsnList ilst = methodNode.instructions;

		int new_start_offset = ilst.indexOf(start);
		int new_end_offset = ilst.indexOf(end);

		for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {

			int start_offset = ilst.indexOf(tcb.start);
			int end_offset = ilst.indexOf(tcb.end);

			if (AsmHelper.offsetBefore(ilst, new_start_offset, start_offset)
					&& AsmHelper.offsetBefore(ilst, start_offset,
							new_end_offset)
					&& AsmHelper.offsetBefore(ilst, new_end_offset, end_offset)) {

				new_start_offset = start_offset;
			} else if (AsmHelper.offsetBefore(ilst, start_offset,
					new_start_offset)
					&& AsmHelper.offsetBefore(ilst, new_start_offset,
							end_offset)
					&& AsmHelper.offsetBefore(ilst, end_offset, new_end_offset)) {

				new_start_offset = end_offset;
			}
		}

		start = ilst.get(new_start_offset);
		end = ilst.get(new_end_offset);

		LabelNode startLabel = getStartLabel(methodNode, start);
		LabelNode endLabel = getEndLabel(methodNode, end);

		return new TryCatchBlockNode(startLabel, endLabel, endLabel, null);
	}

	// TODO respect BEST_EFFORT initialization type in synthetic local variable
	public static void instrument(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars,
			StaticInfo staticInfoHolder, PIResolver piResolver)
			throws DynamicInfoException {
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

		initWeavingLocations(methodNode, snippetMarkings, weaving_start,
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

		sourceAnalyzer = StackUtil.getSourceAnalyzer();

		try {
			sourceAnalyzer.analyze(classNode.name, methodNode);
		} catch (AnalyzerException e) {
			throw new DiSLFatalException("Cause by AnalyzerException : \n"
					+ e.getMessage());
		}

		sourceFrames = sourceAnalyzer.getFrames();

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

					// exception handler will discard the stack and push the
					// exception object. Thus, before entering this snippet,
					// weaver must backup the stack and restore when exiting
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
					AbstractInsnNode[] instr_array = newlst.toArray();

					fixPseudoVar(snippet, region, newlst, staticInfoHolder);
					passesConstsToDynamicAnalysis(snippet, newlst);
					fixDynamicInfo(snippet, region, sourceFrames[index],
							methodNode, newlst);
					fixLocalIndex(methodNode, newlst);
										
					weavingProcessor(methodNode, piResolver, snippet, region,
							newlst, code.getInvokedProcessors().keySet(),
							instr_array, sourceFrames[index]);

					methodNode.instructions.insertBefore(loc, newlst);
					methodNode.tryCatchBlocks.addAll(clone.getTryCatchBlocks());

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
						
						// backup and restore the stack
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
						AbstractInsnNode[] instr_array = newlst.toArray();
						
						fixPseudoVar(snippet, region, newlst, staticInfoHolder);
						passesConstsToDynamicAnalysis(snippet, newlst);
						fixDynamicInfo(snippet, region, sourceFrames[index],
								methodNode, newlst);
						fixLocalIndex(methodNode, newlst);
						
						weavingProcessor(methodNode, piResolver, snippet,
								region, newlst, code.getInvokedProcessors()
										.keySet(), instr_array,
								sourceFrames[index]);
						
						methodNode.instructions.insert(loc, newlst);
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
					// after-throwing inserts the snippet once, and marks
					// the start and the very end as the scope
					int last_index = -1;
					AbstractInsnNode last_exit = null;

					for (AbstractInsnNode exit : region.getEnds()) {

						int index = stack_end.get(exit);

						if (last_index < index) {
							last_index = index;
							last_exit = exit;
						}
					}
					
					if (last_index == -1) {
						continue;
					}

					SnippetCode clone = code.clone();
					InsnList newlst = clone.getInstructions();
					AbstractInsnNode[] instr_array = newlst.toArray();

					fixPseudoVar(snippet, region, newlst, staticInfoHolder);
					passesConstsToDynamicAnalysis(snippet, newlst);
					fixDynamicInfo(snippet, region, sourceFrames[last_index],
							methodNode, newlst);
					fixLocalIndex(methodNode, newlst);

					weavingProcessor(methodNode, piResolver, snippet, region,
							newlst, code.getInvokedProcessors().keySet(),
							instr_array, sourceFrames[last_index]);

					// Create a try-catch clause
					TryCatchBlockNode tcb = getTryCatchBlock(methodNode,
							weaving_start.get(region.getStart()),
							weaving_athrow.get(last_exit));

					methodNode.tryCatchBlocks.add(tcb);
					addExceptionHandlerFrame(methodNode, newlst);
					methodNode.instructions.insert(tcb.handler, newlst);
					methodNode.tryCatchBlocks.addAll(clone.getTryCatchBlocks());
				}
			}
		}

		static2Local(methodNode, syntheticLocalVars);
		AdvancedSorter.sort(methodNode);
	}

}
