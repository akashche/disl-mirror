package ch.usi.dag.disl.weaver.pe;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.cfg.BasicBlock;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;
import ch.usi.dag.disl.util.stack.StackUtil;

public class PartialEvaluator {

	public static MethodNode wrap(InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks) {

		MethodNode method = new MethodNode();

		method.instructions = ilist;
		method.tryCatchBlocks = tryCatchBlocks;
		method.access = Opcodes.ACC_STATIC;
		method.desc = "()V";
		method.maxLocals = MaxCalculator.getMaxLocal(ilist);
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

	private static boolean branchPrediction(CtrlFlowGraph cfg, InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks,
			Map<AbstractInsnNode, Frame<ConstValue>> frames,
			ConstInterpreter interpreter) {

		boolean isOptimized = false;

		for (BasicBlock bb : cfg.getNodes()) {

			AbstractInsnNode instr = AsmHelper.skipVirualInsns(bb.getExit(),
					false);
			int opcode = instr.getOpcode();
			Frame<ConstValue> frame = frames.get(instr);

			switch (instr.getType()) {
			case AbstractInsnNode.JUMP_INSN: {

				ConstValue result = null;

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

		return removeUselessBB(cfg, ilist, tryCatchBlocks) | isOptimized;
	}

	private static boolean removeUselessBB(CtrlFlowGraph cfg, InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks) {

		boolean isOptimized = false;
		boolean changed = true;
		List<BasicBlock> connected = new LinkedList<BasicBlock>(cfg.getNodes());

		connected.remove(cfg.getBB(ilist.getFirst()));

		for (TryCatchBlockNode tcb : tryCatchBlocks) {
			connected.remove(cfg.getBB(tcb.start));
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

	private static boolean removeUselessBranch(InsnList ilist) {

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

	private static boolean removeUselessTCB(CtrlFlowGraph cfg, InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks) {

		boolean isOptimized = false;

		for (TryCatchBlockNode tcb : tryCatchBlocks) {
			if (AsmHelper.skipVirualInsns(tcb.start, true) == AsmHelper
					.skipVirualInsns(tcb.end, true)) {
				tryCatchBlocks.remove(tcb);
				isOptimized |= removeUselessBB(cfg, ilist, tryCatchBlocks);
			}
		}

		return isOptimized;
	}

	public static boolean evaluate(InsnList ilist,
			List<TryCatchBlockNode> tryCatchBlocks) {

		boolean isOptimized = false;
		boolean flag = withoutReturn(ilist);

		if (flag) {
			ilist.add(new InsnNode(Opcodes.RETURN));
		}

		MethodNode method = wrap(ilist, tryCatchBlocks);

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

		CtrlFlowGraph cfg = CtrlFlowGraph.build(method);

		isOptimized |= branchPrediction(cfg, ilist, tryCatchBlocks, frames,
				interpreter);
		isOptimized |= removeUselessBranch(ilist);
		isOptimized |= removeUselessTCB(cfg, ilist, tryCatchBlocks);

		if (flag) {
			ilist.remove(ilist.getLast());
		}

		return isOptimized;
	}
}
