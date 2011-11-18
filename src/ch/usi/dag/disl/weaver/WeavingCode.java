package ch.usi.dag.disl.weaver;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.DynamicInfoException;
import ch.usi.dag.disl.processor.ArgumentContext;
import ch.usi.dag.disl.processor.ProcessorMode;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.processor.generator.ProcInstance;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetCode;
import ch.usi.dag.disl.staticcontext.generator.SCGenerator;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.stack.StackUtil;

public class WeavingCode {

	private WeavingInfo info;

	private MethodNode method;

	private SnippetCode code;
	private InsnList iList;

	private AbstractInsnNode[] iArray;

	private Snippet snippet;
	private Shadow region;
	private int index;
	private int maxLocals;

	public WeavingCode(WeavingInfo weavingInfo, SnippetCode src,
			MethodNode method, Snippet snippet, Shadow region, int index) {

		info = weavingInfo;

		code = src.clone();

		iList = code.getInstructions();
		iArray = iList.toArray();

		this.method = method;
		this.snippet = snippet;
		this.region = region;
		this.index = index;

		for (AbstractInsnNode instr : iArray) {

			if (instr instanceof VarInsnNode) {

				VarInsnNode varInstr = (VarInsnNode) instr;

				switch (varInstr.getOpcode()) {
				case Opcodes.LLOAD:
				case Opcodes.DLOAD:
				case Opcodes.LSTORE:
				case Opcodes.DSTORE:

					if ((varInstr.var + 2) > maxLocals) {
						maxLocals = varInstr.var + 2;
					}

					break;

				default:
					if ((varInstr.var + 1) > maxLocals) {
						maxLocals = varInstr.var + 1;
					}

					break;
				}
			} else if (instr instanceof IincInsnNode) {

				IincInsnNode iinc = (IincInsnNode) instr;

				if ((iinc.var + 1) > maxLocals) {
					maxLocals = iinc.var + 1;
				}
			}
		}
	}

	// Search for an instruction sequence that match the pattern
	// of fetching static information.
	public void fixStaticInfo(SCGenerator staticInfoHolder) {

		for (AbstractInsnNode instr : iList.toArray()) {

			AbstractInsnNode previous = instr.getPrevious();
			// Static information are represented by a static function call with
			// a null as its parameter.
			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL || previous == null
					|| previous.getOpcode() != Opcodes.ALOAD) {
				continue;
			}

			MethodInsnNode invocation = (MethodInsnNode) instr;

			if (staticInfoHolder.contains(region, invocation.owner,
					invocation.name)) {

				Object const_var = staticInfoHolder.get(region,
						invocation.owner, invocation.name);

				if (const_var != null) {
					// Insert a ldc instruction
					code.getInstructions().insert(instr,
							AsmHelper.loadConst(const_var));
				} else {
					// push null onto the stack
					code.getInstructions().insert(instr,
							new InsnNode(Opcodes.ACONST_NULL));
				}

				// remove the pseudo instructions
				iList.remove(previous);
				iList.remove(instr);
			}
		}
	}

	private void preFixDynamicInfoCheck() throws DynamicInfoException {

		for (AbstractInsnNode instr : iList.toArray()) {

			// it is invocation...
			if (instr.getOpcode() != Opcodes.INVOKEINTERFACE) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			// ... of dynamic context
			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			if (invoke.name.equals("thisValue")) {
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
				throw new DynamicInfoException("In snippet "
						+ snippet.getOriginClassName() + "."
						+ snippet.getOriginMethodName()
						+ " - pass the first (pos)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: stackValue(1, int.class)");
			}

			// second operand test
			if (AsmHelper.getClassType(secondOperand) == null) {
				throw new DynamicInfoException("In snippet "
						+ snippet.getOriginClassName() + "."
						+ snippet.getOriginMethodName()
						+ " - pass the second (type)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: stackValue(1, int.class)");
			}
		}
	}

	// Search for an instruction sequence that stands for a request for dynamic
	// information, and replace them with a load instruction.
	// NOTE that if the user requests for the stack value, some store
	// instructions will be inserted to the target method, and new local slot
	// will be used for storing this.
	public void fixDynamicInfo() throws DynamicInfoException {

		preFixDynamicInfoCheck();

		int max = AsmHelper.calcMaxLocal(iList);
		Frame<BasicValue> basicframe = info.getBasicFrame(index);
		Frame<SourceValue> sourceframe = info.getSourceFrame(index);

		for (AbstractInsnNode instr : iList.toArray()) {
			// pseudo function call
			if (instr.getOpcode() != Opcodes.INVOKEINTERFACE) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			AbstractInsnNode prev = instr.getPrevious();

			if (invoke.name.equals("thisValue")) {

				if ((method.access & Opcodes.ACC_STATIC) == 0) {
					iList.insert(instr, new VarInsnNode(Opcodes.ALOAD,
							-method.maxLocals));
				} else {
					iList.insert(instr, new InsnNode(Opcodes.ACONST_NULL));
				}

				iList.remove(invoke);
				iList.remove(prev);
				continue;
			}

			AbstractInsnNode next = instr.getNext();

			// parsing:
			// aload dynamic_info
			// iconst
			// ldc class
			// invoke (current instruction)
			// [checkcast]
			// [invoke]
			int operand = AsmHelper.getIConstOperand(prev.getPrevious());
			Type t = AsmHelper.getClassType(prev);

			if (invoke.name.equals("stackValue")) {
				int sopcode = t.getOpcode(Opcodes.ISTORE);
				int lopcode = t.getOpcode(Opcodes.ILOAD);

				// index should be less than the stack height
				if (operand >= basicframe.getStackSize() || operand < 0) {
					throw new DiSLFatalException("Illegal access of index "
							+ operand + " on a stack with "
							+ basicframe.getStackSize() + " operands");
				}

				// Type checking
				Type targetType = StackUtil
						.getStackByIndex(basicframe, operand).getType();

				if (t.getSort() != targetType.getSort()) {
					throw new DiSLFatalException("Unwanted type \""
							+ targetType + "\", while user needs \"" + t + "\"");
				}

				// store the stack value without changing the semantic
				int size = StackUtil.dupStack(sourceframe, method, operand,
						sopcode, method.maxLocals + max);
				// load the stack value
				iList.insert(instr, new VarInsnNode(lopcode, max));
				max += size;
			}
			// TRICK: the following two situation will generate a VarInsnNode
			// with a negative local slot. And it will be updated in
			// method fixLocalIndex
			else if (invoke.name.equals("methodArgumentValue")) {

				int slot = AsmHelper.getInternalParamIndex(method, operand);

				// index should be less than the size of local variables
				if (slot >= basicframe.getLocals() || slot < 0) {
					throw new DiSLFatalException("Illegal access of index "
							+ slot + " while the size of local variable is "
							+ basicframe.getLocals());
				}

				// Type checking
				Type targetType = basicframe.getLocal(slot).getType();

				if (t.getSort() != targetType.getSort()) {
					throw new DiSLFatalException("Unwanted type \""
							+ targetType + "\", while user needs \"" + t + "\"");
				}

				iList.insert(instr, new VarInsnNode(t.getOpcode(Opcodes.ILOAD),
						slot - method.maxLocals));
			} else if (invoke.name.equals("localVariableValue")) {

				// index should be less than the size of local variables
				if (operand >= basicframe.getLocals() || operand < 0) {
					throw new DiSLFatalException("Illegal access of index "
							+ operand + " while the size of local variable is "
							+ basicframe.getLocals());
				}

				// Type checking
				Type targetType = basicframe.getLocal(operand).getType();

				if (t.getSort() != targetType.getSort()) {
					throw new DiSLFatalException("Unwanted type \""
							+ targetType + "\", while user needs \"" + t + "\"");
				}

				iList.insert(instr, new VarInsnNode(t.getOpcode(Opcodes.ILOAD),
						operand - method.maxLocals));
			}

			// remove invoke
			iList.remove(instr);

			// remove aload, iconst, ldc
			for (int i = 0; i < 3; i++) {
				instr = prev;
				prev = prev.getPrevious();
				iList.remove(instr);
			}

			// remove checkcast, invoke
			if (next.getOpcode() == Opcodes.CHECKCAST) {
				instr = next;
				next = next.getNext();
				iList.remove(instr);

				if (next.getOpcode() == Opcodes.INVOKEVIRTUAL) {
					iList.remove(next);
				}
			}
		}
	}

	// Fix the stack operand index of each stack-based instruction
	// according to the maximum number of locals in the target method node.
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	public void fixLocalIndex() {
		method.maxLocals = fixLocalIndex(iList, method.maxLocals);
	}

	private int fixLocalIndex(InsnList src, int offset) {
		int max = offset;

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr instanceof VarInsnNode) {

				VarInsnNode varInstr = (VarInsnNode) instr;
				varInstr.var += offset;

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
				iinc.var += offset;

				if ((iinc.var + 1) > max) {
					max = iinc.var + 1;
				}
			}
		}

		return max;
	}

	private void fixArgumentContext(InsnList instructions, int position,
			int totalCount, Type type) {

		for (AbstractInsnNode instr : instructions.toArray()) {

			AbstractInsnNode previous = instr.getPrevious();

			if (instr.getOpcode() != Opcodes.INVOKEINTERFACE
					|| previous == null
					|| previous.getOpcode() != Opcodes.ALOAD) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			if (!invoke.owner.equals(Type
					.getInternalName(ArgumentContext.class))) {
				continue;
			}

			if (invoke.name.equals("position")) {
				instructions.insert(instr, AsmHelper.loadConst(position));
			} else if (invoke.name.equals("totalCount")) {
				instructions.insert(instr, AsmHelper.loadConst(totalCount));
			} else if (invoke.name.equals("typeDescriptor")) {
				instructions
						.insert(instr, AsmHelper.loadConst(type.toString()));
			}

			// remove the pseudo instructions
			instructions.remove(previous);
			instructions.remove(instr);
		}
	}

	// combine processors into an instruction list
	// NOTE that these processors are for the current method
	private InsnList procInMethod(ProcInstance processor) {

		InsnList ilist = new InsnList();

		for (ProcMethodInstance processorMethod : processor.getMethods()) {

			Code code = processorMethod.getCode().clone();
			InsnList instructions = code.getInstructions();

			int position = processorMethod.getArgPos();
			int totalCount = processorMethod.getArgsCount();
			Type type = processorMethod.getArgType().getASMType();

			fixArgumentContext(instructions, position, totalCount, type);

			AbstractInsnNode start = instructions.getFirst();

			VarInsnNode target = new VarInsnNode(
					type.getOpcode(Opcodes.ISTORE), 0);
			instructions.insertBefore(start, target);

			maxLocals = fixLocalIndex(instructions, maxLocals);

			instructions.insertBefore(
					target,
					new VarInsnNode(type.getOpcode(Opcodes.ILOAD), AsmHelper
							.getInternalParamIndex(method,
									processorMethod.getArgPos())
							- method.maxLocals));

			ilist.add(instructions);
			method.tryCatchBlocks.addAll(code.getTryCatchBlocks());
		}

		return ilist;
	}

	// combine processors into an instruction list
	// NOTE that these processors are for the callee
	private InsnList procBeforeInvoke(ProcInstance processor, int index) {

		Frame<SourceValue> frame = info.getSourceFrame(index);
		InsnList ilist = new InsnList();

		for (ProcMethodInstance processorMethod : processor.getMethods()) {

			Code code = processorMethod.getCode().clone();
			InsnList instructions = code.getInstructions();

			int position = processorMethod.getArgPos();
			int totalCount = processorMethod.getArgsCount();
			Type type = processorMethod.getArgType().getASMType();

			fixArgumentContext(instructions, position, totalCount, type);

			SourceValue source = StackUtil.getStackByIndex(frame, totalCount
					- 1 - position);
			int sopcode = type.getOpcode(Opcodes.ISTORE);

			for (AbstractInsnNode itr : source.insns) {
				method.instructions.insert(itr, new VarInsnNode(sopcode,
				// TRICK: the value has to be set properly because
				// method code will be not adjusted by fixLocalIndex
						method.maxLocals + maxLocals));
				method.instructions.insert(itr, new InsnNode(
						type.getSize() == 2 ? Opcodes.DUP2 : Opcodes.DUP));
			}

			maxLocals = fixLocalIndex(instructions, maxLocals);
			ilist.add(instructions);
			method.tryCatchBlocks.addAll(code.getTryCatchBlocks());
		}

		return ilist;
	}

	// replace processor-applying pseudo invocation with processors
	public void fixProcessor(PIResolver piResolver) {

		for (int i : code.getInvokedProcessors().keySet()) {

			AbstractInsnNode instr = iArray[i];
			ProcInstance processor = piResolver.get(region, i);

			if (processor != null) {
				if (processor.getProcApplyType() == ProcessorMode.CALLSITE_ARGS) {
					iList.insert(instr, procBeforeInvoke(processor, index));
				} else {
					iList.insert(instr, procInMethod(processor));
				}
			}

			// remove pseudo invocation
			iList.remove(instr.getPrevious().getPrevious());
			iList.remove(instr.getPrevious());
			iList.remove(instr);
		}
	}

	public InsnList getiList() {
		return iList;
	}

	public List<TryCatchBlockNode> getTCBs() {
		return code.getTryCatchBlocks();
	}

	public void transform(SCGenerator staticInfoHolder, PIResolver piResolver)
			throws DynamicInfoException {
		fixProcessor(piResolver);
		fixStaticInfo(staticInfoHolder);
		fixDynamicInfo();
		fixLocalIndex();
	}
}
