package ch.usi.dag.disl.weaver.pe;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;
import ch.usi.dag.disl.util.stack.StackUtil;

public class PartialEvaluator {

	private static MethodNode wrap(InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks, String desc, int access) {

		MethodNode method = new MethodNode();

		method.instructions = ilist;
		method.tryCatchBlocks = tryCatchBlocks;
		method.access = access;
		method.desc = desc;
		method.maxLocals = MaxCalculator.getMaxLocal(ilist, desc, access);
		method.maxStack = MaxCalculator.getMaxStack(ilist, tryCatchBlocks);

		return method;
	}

	private static boolean withoutReturn(InsnList ilist) {

		for (AbstractInsnNode instr : ilist.toArray()) {

			if (instr.getOpcode() == Opcodes.GOTO) {
				JumpInsnNode jump = (JumpInsnNode) instr;

				if (AsmHelper.skipVirualInsns(jump.label, true) == null) {
					return true;
				}
			}
		}

		int lastOp = AsmHelper.skipVirualInsns(ilist.getLast(), false)
				.getOpcode();

		if (lastOp != Opcodes.ATHROW || AsmHelper.isReturn(lastOp)) {
			return true;
		}

		return false;
	}

	private static boolean insertLoadConstant(InsnList ilist,
			AbstractInsnNode location, Object cst) {

		if (cst == null) {
			return false;
		}

		if (cst == ConstValue.NULL) {
			ilist.insertBefore(location, new InsnNode(Opcodes.ACONST_NULL));
			return true;
		}

		ilist.insertBefore(location, AsmHelper.loadConst(cst));
		return true;
	}

	private static boolean replaceLoadWithLDC(InsnList ilist,
			ConstInterpreter interpreter,
			Map<AbstractInsnNode, Frame<ConstValue>> frames) {

		boolean isOptimized = false;

		for (AbstractInsnNode instr : ilist.toArray()) {

			Frame<ConstValue> frame = frames.get(instr);

			if (frame == null) {
				continue;
			}

			if (ConstInterpreter.mightBeUnaryConstOperation(instr)) {

				ConstValue value = StackUtil.getStackByIndex(frame, 0);
				Object cst = interpreter.unaryOperation(instr, value).cst;

				if (insertLoadConstant(ilist, instr, cst)) {

					ilist.insertBefore(instr.getPrevious(), new InsnNode(
							value.size == 1 ? Opcodes.POP : Opcodes.POP2));
					ilist.remove(instr);
					isOptimized = true;
				}

				continue;
			} else if (ConstInterpreter.mightBeBinaryConstOperation(instr)) {
				
				ConstValue value1 = StackUtil.getStackByIndex(frame, 1);
				ConstValue value2 = StackUtil.getStackByIndex(frame, 0);
				Object cst = interpreter.binaryOperation(instr, value1, value2).cst;
				
				if (insertLoadConstant(ilist, instr, cst)) {

					ilist.insertBefore(instr.getPrevious(), new InsnNode(
							value2.size == 1 ? Opcodes.POP : Opcodes.POP2));
					ilist.insertBefore(instr.getPrevious(), new InsnNode(
							value1.size == 1 ? Opcodes.POP : Opcodes.POP2));
					ilist.remove(instr);
					isOptimized = true;
				}

				continue;
			}

			switch (instr.getOpcode()) {
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
				if (insertLoadConstant(ilist, instr,
						frame.getLocal(((VarInsnNode) instr).var).cst)) {
					ilist.remove(instr);
					isOptimized = true;
				}

				break;

			default:
				break;
			}
		}

		return isOptimized;
	}

	private static boolean loadAfterStore(BasicBlock bb,
			AbstractInsnNode instr, int var) {

		while (instr != bb.getExit()) {
			switch (instr.getOpcode()) {
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD:
				if (((VarInsnNode) instr).var == var) {
					return true;
				}
			default:
				instr = instr.getNext();
			}
		}

		return false;
	}

	private static boolean deadStore(CtrlFlowGraph cfg, VarInsnNode store) {

		BasicBlock bb = cfg.getBB(store);

		if (bb == null) {
			return false;
		}

		if (loadAfterStore(bb, store, store.var)){
			return false;
		}
		
		HashSet<BasicBlock> visited = new HashSet<BasicBlock>();
		Queue<BasicBlock> unprocessed = new LinkedList<BasicBlock>(
				bb.getSuccessors());

		while (!unprocessed.isEmpty()) {
			BasicBlock next = unprocessed.poll();

			if (visited.contains(next)) {
				continue;
			}

			if (loadAfterStore(next, next.getEntrance(), store.var)) {
				return false;
			}

			visited.add(next);
		}
		
		return true;
	}

	private static boolean removeDeadStore(MethodNode method, InsnList ilist) {

		CtrlFlowGraph cfg = CtrlFlowGraph.build(method);
		boolean isOptimized = false;

		for (AbstractInsnNode instr : ilist.toArray()) {

			switch (instr.getOpcode()) {
			case Opcodes.ISTORE:
			case Opcodes.ASTORE:
			case Opcodes.FSTORE:

				if (deadStore(cfg, (VarInsnNode) instr)) {

					ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
					ilist.remove(instr);
					isOptimized = true;
				}

				break;

			case Opcodes.DSTORE:
			case Opcodes.LSTORE:

				if (deadStore(cfg, (VarInsnNode) instr)) {

					ilist.insertBefore(instr, new InsnNode(Opcodes.POP2));
					ilist.remove(instr);
					isOptimized = true;
				}

				break;
			default:
				break;
			}
		}

		return isOptimized;
	}

	private static boolean conditionalReduction(MethodNode method,
			InsnList ilist, List<TryCatchBlockNode> tryCatchBlocks,
			Map<AbstractInsnNode, Frame<ConstValue>> frames,
			ConstInterpreter interpreter) {

		CtrlFlowGraph cfg = CtrlFlowGraph.build(method);
		boolean isOptimized = false;

		for (BasicBlock bb : cfg.getNodes()) {

			AbstractInsnNode instr = AsmHelper.skipVirualInsns(bb.getExit(),
					false);
			int opcode = instr.getOpcode();
			Frame<ConstValue> frame = frames.get(instr);

			switch (instr.getType()) {
			case AbstractInsnNode.JUMP_INSN: {

				ConstValue result = null;
				boolean popTwice = false;

				switch (opcode) {
				case Opcodes.JSR:
				case Opcodes.GOTO:
					continue;

				case Opcodes.IF_ICMPEQ:
				case Opcodes.IF_ICMPNE:
				case Opcodes.IF_ICMPLT:
				case Opcodes.IF_ICMPGE:
				case Opcodes.IF_ICMPGT:
				case Opcodes.IF_ICMPLE:
				case Opcodes.IF_ACMPEQ:
				case Opcodes.IF_ACMPNE: {

					ConstValue value1 = StackUtil.getStackByIndex(frame, 1);
					ConstValue value2 = StackUtil.getStackByIndex(frame, 0);
					result = interpreter.binaryOperation(instr, value1, value2);
					popTwice = true;
					break;
				}

				default: {

					ConstValue value = StackUtil.getStackByIndex(frame, 0);
					result = interpreter.unaryOperation(instr, value);
					break;
				}
				}

				if (result.cst == null) {
					continue;
				}

				if ((Boolean) result.cst) {

					BasicBlock successor = cfg.getBB(instr.getNext());
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);

					if (popTwice) {
						ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
					}

					ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
					ilist.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO,
							((JumpInsnNode) instr).label));
					bb.setExit(instr.getPrevious());
					ilist.remove(instr);
				} else {

					BasicBlock successor = cfg
							.getBB(((JumpInsnNode) instr).label);
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);

					if (popTwice) {
						ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
					}

					ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
					bb.setExit(instr.getPrevious());
					ilist.remove(instr);
				}

				isOptimized = true;
				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) instr;

				ConstValue value = StackUtil.getStackByIndex(frame, 0);

				if (value.cst == null) {
					continue;
				}

				int index = lsin.keys.indexOf(value.cst);
				LabelNode label = null;

				if (index >= 0) {

					BasicBlock successor = cfg.getBB(lsin.dflt);
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);
				} else {
					label = lsin.dflt;
				}

				for (int i = 0; i < lsin.labels.size(); i++) {

					if (i == index) {
						label = lsin.labels.get(i);
						continue;
					}

					BasicBlock successor = cfg.getBB(lsin.labels.get(i));
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);
				}

				ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
				ilist.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO, label));
				bb.setExit(instr.getPrevious());
				ilist.remove(instr);
				isOptimized = true;
				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) instr;

				ConstValue value = StackUtil.getStackByIndex(frame, 0);

				if (value.cst == null) {
					continue;
				}

				int index = (Integer) value.cst;
				LabelNode label = null;

				if (index < tsin.min && index > tsin.max) {

					BasicBlock successor = cfg.getBB(tsin.dflt);
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);
				} else {
					label = tsin.dflt;
				}

				for (int i = tsin.min; i <= tsin.max; i++) {

					if (i == index) {
						label = tsin.labels.get(i - tsin.min);
						continue;
					}

					BasicBlock successor = cfg.getBB(tsin.labels.get(i
							- tsin.min));
					bb.getSuccessors().remove(successor);
					successor.getPredecessors().remove(bb);
				}

				ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
				ilist.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO, label));
				bb.setExit(instr.getPrevious());
				ilist.remove(instr);
				isOptimized = true;
				break;
			}

			default:
				break;
			}
		}

		return removeUnusedBB(cfg, ilist, tryCatchBlocks) | isOptimized;
	}

	private static boolean removeUnusedBB(CtrlFlowGraph cfg, InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks) {

		boolean isOptimized = false;
		boolean changed = true;
		List<BasicBlock> connected = new LinkedList<BasicBlock>(cfg.getNodes());

		connected.remove(cfg.getBB(ilist.getFirst()));

		for (TryCatchBlockNode tcb : tryCatchBlocks) {
			connected.remove(cfg.getBB(tcb.handler));
		}

		while (changed) {

			changed = false;
			List<BasicBlock> removed = new LinkedList<BasicBlock>();

			for (BasicBlock bb : connected) {

				if (bb.getPredecessors().size() > 0) {
					continue;
				}

				changed = true;
				AbstractInsnNode prev = null;
				AbstractInsnNode iter = bb.getEntrance();

				while (prev != bb.getExit()) {
					prev = iter;
					iter = iter.getNext();

					int opcode = prev.getOpcode();

					if (opcode != -1 || opcode != Opcodes.RETURN) {
						isOptimized = true;
						ilist.remove(prev);
					}
				}

				for (BasicBlock successor : bb.getSuccessors()) {
					successor.getPredecessors().remove(bb);
				}

				removed.add(bb);
			}

			connected.removeAll(removed);
		}

		return isOptimized;
	}

	private static boolean removeUnusedJump(InsnList ilist) {

		boolean isOptimized = false;

		for (AbstractInsnNode instr : ilist.toArray()) {

			int opcode = instr.getOpcode();

			switch (instr.getType()) {
			case AbstractInsnNode.JUMP_INSN: {

				if (opcode == Opcodes.JSR) {
					continue;
				}

				if (AsmHelper.skipVirualInsns(((JumpInsnNode) instr).label,
						false) != instr) {
					continue;
				}

				if (opcode != Opcodes.GOTO) {
					ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
				}

				ilist.remove(instr);
				isOptimized = true;
				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) instr;
				boolean flag = false;

				for (LabelNode label : lsin.labels) {
					if (AsmHelper.skipVirualInsns(label, false) != instr) {
						flag = true;
						continue;
					}
				}

				if (flag
						|| AsmHelper.skipVirualInsns(lsin.dflt, false) != instr) {
					continue;
				}

				ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
				ilist.remove(instr);
				isOptimized = true;
				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) instr;

				boolean flag = false;

				for (LabelNode label : tsin.labels) {
					if (AsmHelper.skipVirualInsns(label, false) != instr) {
						flag = true;
						continue;
					}
				}

				if (flag
						|| AsmHelper.skipVirualInsns(tsin.dflt, false) != instr) {
					continue;
				}

				ilist.insertBefore(instr, new InsnNode(Opcodes.POP));
				ilist.remove(instr);
				isOptimized = true;
				break;
			}

			default:
				break;
			}
		}

		return isOptimized;
	}

	private static boolean removeUnusedHandler(MethodNode method,
			InsnList ilist, List<TryCatchBlockNode> tryCatchBlocks) {

		CtrlFlowGraph cfg = CtrlFlowGraph.build(method);
		boolean isOptimized = false;

		for (TryCatchBlockNode tcb : tryCatchBlocks) {
			if (AsmHelper.skipVirualInsns(tcb.start, true) == AsmHelper
					.skipVirualInsns(tcb.end, true)) {
				tryCatchBlocks.remove(tcb);
				isOptimized |= removeUnusedBB(cfg, ilist, tryCatchBlocks);
			}
		}

		return isOptimized;
	}
	
	private static void removeInvocation(InsnList ilist, MethodInsnNode min) {

		if (InvocationInterpreter.getInstance().isRegistered(min)) {

			String desc = min.desc;

			if (min.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				ilist.insert(min, new InsnNode(Opcodes.POP));
			}

			for (Type arg : Type.getArgumentTypes(desc)) {
				ilist.insert(min, new InsnNode(
						arg.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
			}
		}
	}
	
	private static void removeAllocation(InsnList ilist, AbstractInsnNode next,
			Map<AbstractInsnNode, Frame<SourceValue>> frames) {

		if (next.getOpcode() != Opcodes.DUP) {
			return;
		}

		for (AbstractInsnNode instr : ilist.toArray()) {
			if (instr.getOpcode() == Opcodes.INVOKESPECIAL) {
				Type[] args = Type
						.getArgumentTypes(((MethodInsnNode) instr).desc);
				Frame<SourceValue> frame = frames.get(instr);
				Set<AbstractInsnNode> sources = StackUtil.getStackByIndex(
						frame, args.length).insns;

				if (sources.contains(next)) {

					for (Type arg : args) {
						ilist.insert(instr,
								new InsnNode(arg.getSize() == 2 ? Opcodes.POP2
										: Opcodes.POP));
					}

					ilist.remove(instr);
				}
			}
		}
		
		ilist.remove(next);
	}

	private static boolean uselessWhenPop(final AbstractInsnNode insn) {

		switch (insn.getOpcode()) {
		case Opcodes.ACONST_NULL:
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
		case Opcodes.LDC:
		case Opcodes.NEW:
		case Opcodes.ILOAD:
		case Opcodes.LLOAD:
		case Opcodes.FLOAD:
		case Opcodes.DLOAD:
		case Opcodes.ALOAD:
			return true;
		default:
			return false;
		}
	}

	private static boolean removePop(MethodNode method, InsnList ilist) {

		Analyzer<SourceValue> sourceAnalyzer = StackUtil.getSourceAnalyzer();

		try {
			sourceAnalyzer.analyze(PartialEvaluator.class.getName(), method);
		} catch (AnalyzerException e) {
			e.printStackTrace();
			throw new DiSLFatalException("Cause by AnalyzerException : \n"
					+ e.getMessage());
		}
		
		Map<AbstractInsnNode, Frame<SourceValue>> frames = 
				new HashMap<AbstractInsnNode, Frame<SourceValue>>();

		for (int i = 0; i < ilist.size(); i++) {
			frames.put(ilist.get(i), sourceAnalyzer.getFrames()[i]);
		}

		boolean isOptimized = false;

		for (AbstractInsnNode instr : ilist.toArray()) {

			int opcode = instr.getOpcode();

			if (opcode != Opcodes.POP && opcode != Opcodes.POP2) {
				continue;
			}

			Frame<SourceValue> frame = frames.get(instr);

			if (frame == null) {
				continue;
			}

			boolean flag = true;

			Set<AbstractInsnNode> sources = 
					StackUtil.getStackByIndex(frame, 0).insns;

			for (AbstractInsnNode source : sources) {

				if (!(uselessWhenPop(source) || InvocationInterpreter
						.getInstance().isRegistered(source))) {
					flag = false;
					break;
				}
			}

			if (!flag) {
				continue;
			}


			for (AbstractInsnNode source : sources) {

				if (source instanceof MethodInsnNode) {
					removeInvocation(ilist, (MethodInsnNode) source);
				}

				if (source.getOpcode() == Opcodes.NEW) {
					removeAllocation(ilist, source.getNext(), frames);
				}

				ilist.remove(source);
			}

			ilist.remove(instr);
			isOptimized = true;
		}

		return isOptimized;
	}

	private static List<String> primitiveTypes;

	static {
		primitiveTypes = new LinkedList<String>();
		primitiveTypes.add("java/lang/Boolean");
		primitiveTypes.add("java/lang/Byte");
		primitiveTypes.add("java/lang/Character");
		primitiveTypes.add("java/lang/Double");
		primitiveTypes.add("java/lang/Float");
		primitiveTypes.add("java/lang/Integer");
		primitiveTypes.add("java/lang/Long");
	}

	private static boolean removeBoxingAndUnboxing(InsnList ilist) {

		boolean isOptimized = false;

		for (AbstractInsnNode instr : ilist.toArray()) {

			AbstractInsnNode prev = instr.getPrevious();

			if (prev == null || (prev.getOpcode() != Opcodes.INVOKESTATIC)
					|| (instr.getOpcode() != Opcodes.INVOKEVIRTUAL)) {
				continue;
			}

			MethodInsnNode valueOf = (MethodInsnNode) prev;
			MethodInsnNode toValue = (MethodInsnNode) instr;

			if (!(primitiveTypes.contains(valueOf.owner)
					&& valueOf.owner.equals(toValue.owner) && valueOf.name
						.equals("valueOf")) && toValue.name.endsWith("Value")) {
				continue;
			}

			if (!Type.getArgumentTypes(valueOf.desc)[0].equals(Type
					.getReturnType(toValue.desc))) {
				continue;
			}

			ilist.remove(prev);
			ilist.remove(instr);
			isOptimized = true;
		}

		return isOptimized;
	}

	public static boolean evaluate(InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks, String desc, int access) {

		boolean isOptimized = false, removed = false;
		boolean flag = withoutReturn(ilist);

		if (flag) {
			ilist.add(new InsnNode(Opcodes.RETURN));
		}

		String newDesc = desc.substring(0, desc.lastIndexOf(')')) + ")V";
		MethodNode method = wrap(ilist, tryCatchBlocks, newDesc, access);

		ConstInterpreter interpreter = new ConstInterpreter();
		Analyzer<ConstValue> constAnalyzer = new Analyzer<ConstValue>(
				interpreter);

		try {
			constAnalyzer.analyze(PartialEvaluator.class.getName(), method);
		} catch (AnalyzerException e) {
			throw new DiSLFatalException("Cause by AnalyzerException : \n"
					+ e.getMessage());
		}

		Map<AbstractInsnNode, Frame<ConstValue>> frames = 
				new HashMap<AbstractInsnNode, Frame<ConstValue>>();
		Frame<ConstValue>[] constFrames = constAnalyzer.getFrames();

		for (int i = 0; i < ilist.size(); i++) {
			frames.put(ilist.get(i), constFrames[i]);
		}

		isOptimized |= conditionalReduction(method, ilist, tryCatchBlocks, frames,
				interpreter);

		isOptimized |= replaceLoadWithLDC(ilist, interpreter, frames);
		isOptimized |= removeBoxingAndUnboxing(ilist);
		
		do {
			removed = false;
			removed |= removeDeadStore(method, ilist);
			removed |= removePop(method, ilist);
		} while (removed);

		isOptimized |= removed;
		isOptimized |= removeUnusedJump(ilist);
		isOptimized |= removeUnusedHandler(method, ilist, tryCatchBlocks);

		if (flag) {
			ilist.remove(ilist.getLast());
		}

		return isOptimized;
	}
}
