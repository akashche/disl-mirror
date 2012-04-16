package ch.usi.dag.disl.weaver.splitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.FrameHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;
import ch.usi.dag.disl.weaver.pe.MaxCalculator;

public class Splitter {

	public int idx = 0;

	private ClassNode clazz;
	private MethodNode method;
	private CtrlFlowGraph cfg;

	private LocalVar[] locals;
	private List<SuperBlock> superblocks;

	private Map<AbstractInsnNode, Frame<BasicValue>> frames_mapping;

	public Splitter(ClassNode clazz, MethodNode method) {

		this.clazz = clazz;
		this.method = method;
		this.cfg = CtrlFlowGraph.build(method);

		locals = new LocalVar[method.maxLocals];

		for (int i = 0; i < method.maxLocals; i++) {
			locals[i] = new LocalVar(i);
		}

		frames_mapping = FrameHelper.createBasicMapping(clazz.name, method);
		superblocks = SuperBlock.build(method, cfg, locals, frames_mapping);

		Collections.sort(superblocks);
	}

	public AbstractInsnNode fork(MethodNode newMethod, SuperBlock sb) {

		InsnList src = method.instructions;
		InsnList dst = newMethod.instructions;

		AbstractInsnNode[] src_array = src.toArray();
		AbstractInsnNode[] dst_array = dst.toArray();

		boolean flag = false;
		LabelNode label = new LabelNode();
		InsnList iList = new InsnList();

		for (int i = 0; i < src_array.length; i++) {

			AbstractInsnNode instr_src = src_array[i];
			AbstractInsnNode instr_dst = dst_array[i];

			if (instr_src.equals(sb.start)) {

				src.insertBefore(instr_src, label);
				flag = true;
			}

			if (flag) {
				if (instr_src.getOpcode() != -1) {
					src.remove(instr_src);
					iList.add(instr_src);
				}
			} else {
				if (instr_dst.getOpcode() != -1) {
					dst.remove(instr_dst);
				}
			}

			if (instr_src.equals(sb.end)) {
				flag = false;
			}
		}

		return label;
	}

	public void complete(MethodNode newMethod, SuperBlock sb,
			AbstractInsnNode label) {

		boolean fReturn = SplitterHelper.addReturn(newMethod.instructions);
		boolean fStatic = (method.access & Opcodes.ACC_STATIC) != 0;

		InsnList parent = method.instructions;
		InsnList child = newMethod.instructions;

		AbstractInsnNode parameter = new LabelNode();
		parent.insertBefore(label, parameter);
		AbstractInsnNode aftercall = new LabelNode();
		parent.insert(label, aftercall);

		AbstractInsnNode iReturn = SplitterHelper.getReturn(child);

		Frame<BasicValue> frame_start = frames_mapping.get(sb.start);
		Frame<BasicValue> frame_end = frames_mapping.get(sb.end);

		Map<Integer, Integer> mapping = new HashMap<Integer, Integer>();
		Stack<Type> types = new Stack<Type>();

		Type returnType = Type.VOID_TYPE;
		int down = 0, top = 0;

		for (LocalVar var : sb.usedVar) {

			Type t = frame_start.getLocal(var.index).getType();

			if (t == null || !sb.isParameter(var)) {

				t = frame_end.getLocal(var.index).getType();
				mapping.put(var.index, top);
				top += t.getSize();
				continue;
			}

			types.push(t);
			down -= t.getSize();
			mapping.put(var.index, down);
			parent.insert(parameter, new VarInsnNode(
					t.getOpcode(Opcodes.ILOAD), var.index));

			if (!sb.needReturn(var)) {
				continue;
			}

			if (!returnType.equals(Type.VOID_TYPE)) {
				throw new DiSLFatalException("Method cannot return two values.");
			}

			returnType = t;
			child.insertBefore(iReturn,
					new VarInsnNode(t.getOpcode(Opcodes.ILOAD), var.index));
			child.insertBefore(iReturn,
					new InsnNode(t.getOpcode(Opcodes.IRETURN)));
			child.remove(iReturn);

			parent.insert(label, new VarInsnNode(t.getOpcode(Opcodes.ISTORE),
					var.index));
		}

		if (!fStatic) {

			down--;
			mapping.put(0, down);
			parent.insert(parameter, new VarInsnNode(Opcodes.ALOAD, 0));
		}

		for (Integer key : mapping.keySet()) {
			mapping.put(key, mapping.get(key) - down);
		}

		for (AbstractInsnNode instr : newMethod.instructions.toArray()) {

			if (instr instanceof VarInsnNode) {

				VarInsnNode vin = (VarInsnNode) instr;
				vin.var = mapping.get(vin.var);

			} else if (instr instanceof IincInsnNode) {

				IincInsnNode iin = (IincInsnNode) instr;
				iin.var = mapping.get(iin.var);
			}
		}

		newMethod.name = method.name + "$" + idx++;

		if (!fReturn) {
			returnType = Type.getReturnType(method.desc);
			parent.insert(aftercall,
					new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
		}

		newMethod.desc = Type.getMethodDescriptor(returnType,
				types.toArray(new Type[types.size()]));

		if (fStatic) {
			parent.insertBefore(label, new MethodInsnNode(Opcodes.INVOKESTATIC,
					clazz.name, newMethod.name, newMethod.desc));
		} else {
			parent.insertBefore(label, new MethodInsnNode(
					Opcodes.INVOKEVIRTUAL, clazz.name, newMethod.name,
					newMethod.desc));
		}

		newMethod.maxLocals = MaxCalculator.getMaxLocal(newMethod);
		newMethod.maxStack = MaxCalculator.getMaxStack(newMethod);
	}

	public void split() {

		if (superblocks.size() == 0) {
			return;
		}

		for (SuperBlock sb : superblocks) {

			if ((sb.getWeight() < 100) && sb.splitable()) {

				MethodNode newMethod = new MethodNode();

				newMethod.instructions = AsmHelper
						.cloneInsnList(method.instructions);
				newMethod.tryCatchBlocks = new LinkedList<>();
				newMethod.access = method.access;
				newMethod.exceptions = new LinkedList<>();

				AbstractInsnNode label = fork(newMethod, sb);
				complete(newMethod, sb, label);

				clazz.methods.add(newMethod);
			}
		}
	}

}
