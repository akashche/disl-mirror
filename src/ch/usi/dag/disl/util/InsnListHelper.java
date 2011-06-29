package ch.usi.dag.disl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;

public class InsnListHelper {

	// TODO ! refactor - specific methods should go to Weaver

	public static boolean isReturn(int opcode) {
		return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
	}

	public static void removeReturns(InsnList ilst) {
		// Remove 'return' instruction
		List<AbstractInsnNode> returns = new LinkedList<AbstractInsnNode>();

		for (AbstractInsnNode instr : ilst.toArray()) {
			int opcode = instr.getOpcode();

			if (isReturn(opcode)) {
				returns.add(instr);
			}
		}

		if (returns.size() > 1) {
			// Replace 'return' instructions with 'goto'
			LabelNode label = new LabelNode(new Label());
			ilst.add(label);

			for (AbstractInsnNode instr : returns) {
				ilst.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO, label));
				ilst.remove(instr);
			}
		} else if (returns.size() == 1) {
			ilst.remove(returns.get(0));
		}
	}

	// Make a clone of an instruction list
	public static InsnList cloneList(InsnList src) {

		return cloneList(src, src.getFirst(), src.getLast());
	}

	// NOTE: You should know what you are doing, if you are copying jumps, try,
	// etc. the code doesn't need to be valid
	public static InsnList cloneList(InsnList src, AbstractInsnNode from,
			AbstractInsnNode to) {

		Map<AbstractInsnNode, AbstractInsnNode> map = new HashMap<AbstractInsnNode, AbstractInsnNode>();

		InsnList dst = new InsnList();

		// First iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : src.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = new LabelNode(new Label());
				map.put(instr, label);
			}
		}

		// then copy instructions using clone
		AbstractInsnNode instr = from;
		while (instr != to.getNext()) {

			// special case where we put a new label instead of old one
			if (instr instanceof LabelNode) {

				dst.add(map.get(instr));

				instr = instr.getNext();

				continue;
			}

			dst.add(instr.clone(map));

			instr = instr.getNext();
		}

		return dst;
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
			InsnList newlst = InsnListHelper.cloneList(var.getInitASMCode());
			method.instructions.insertBefore(first, newlst);
			
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

	// Get the first valid mark of a method.
	// For a constructor, the return value will be the instruction after
	// the object initialization.
	public static AbstractInsnNode findFirstValidMark(MethodNode method) {
		AbstractInsnNode first = method.instructions.getFirst();

		// This is not a constructor. Just return the first instruction
		if (!method.name.equals(Constants.CONSTRUCTOR_NAME)) {
			return first;
		}

		// Similar to 'const boolean **trigger' in c.
		final boolean trigger[] = { false };

		AdviceAdapter adapter = new AdviceAdapter(new EmptyVisitor(),
				method.access, method.name, method.desc) {
			@Override
			public void onMethodEnter() {
				trigger[0] = true;
			}
		};

		// Iterate instruction list till the instruction right after the
		// object initialization
		adapter.visitCode();

		for (AbstractInsnNode iterator : method.instructions.toArray()) {
			iterator.accept(adapter);

			if (trigger[0]) {
				first = iterator.getPrevious();
				break;
			}
		}

		return first;
	}

	// Detects if the instruction list contains only return
	public static boolean containsOnlyReturn(InsnList ilst) {

		return isReturn(ilst.getFirst().getOpcode());
	}

	// Make sure an instruction has a valid next-instruction.
	// NOTE that in asm, label might be an AbstractInsnNode. If an instruction
	// is followed with a label which is the end of an instruction list, then
	// it has no next instruction.
	public static boolean hasNext(InsnList instr_lst, int i) {
		if (i < instr_lst.size()) {
			AbstractInsnNode nextInstruction = instr_lst.get(i + 1);
			return nextInstruction == null
					|| !(nextInstruction.getOpcode() == -1 && nextInstruction
							.getNext() == null);
		}

		return false;
	}

	public static boolean isBranch(AbstractInsnNode instruction) {
		int opcode = instruction.getOpcode();

		return instruction instanceof JumpInsnNode
				|| instruction instanceof MethodInsnNode
				|| instruction instanceof LookupSwitchInsnNode
				|| instruction instanceof TableSwitchInsnNode
				|| opcode == Opcodes.ATHROW || opcode == Opcodes.RET
				|| opcode == Opcodes.JSR || opcode == Opcodes.CHECKCAST
				|| (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
	}

	// Get basic blocks of the given method node.
	public static List<AbstractInsnNode> getBasicBlocks(MethodNode method) {
		InsnList instr_lst = method.instructions;

		Set<AbstractInsnNode> bb_begins = new HashSet<AbstractInsnNode>();
		bb_begins.add(instr_lst.getFirst());

		for (int i = 0; i < instr_lst.size(); i++) {
			AbstractInsnNode instruction = instr_lst.get(i);
			int opcode = instruction.getOpcode();

			switch (instruction.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				bb_begins.add(((JumpInsnNode) instruction).label);

				// goto never returns.
				if (opcode != Opcodes.GOTO && hasNext(instr_lst, i)) {
					bb_begins.add(instruction.getNext());
				}

				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				for (Object label : ((LookupSwitchInsnNode) instruction).labels) {
					bb_begins.add((AbstractInsnNode) label);
				}

				bb_begins.add(((LookupSwitchInsnNode) instruction).dflt);
				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				for (Object label : ((TableSwitchInsnNode) instruction).labels) {
					bb_begins.add((AbstractInsnNode) label);
				}

				bb_begins.add(((TableSwitchInsnNode) instruction).dflt);
				break;
			}

			case AbstractInsnNode.METHOD_INSN: {
				// Covers invoke*
				if (hasNext(instr_lst, i)) {
					bb_begins.add(instruction.getNext());
				}
			}

			default:
				if (opcode == Opcodes.CHECKCAST && hasNext(instr_lst, i)) {
					// checkcast might throw an exception.
					bb_begins.add(instruction.getNext());
				}
				break;
			}
		}

		for (Object try_catch_block : method.tryCatchBlocks) {
			bb_begins.add(((TryCatchBlockNode) try_catch_block).handler);
		}

		// Sort
		List<AbstractInsnNode> bb_list = new ArrayList<AbstractInsnNode>();

		for (AbstractInsnNode instruction : instr_lst.toArray()) {
			if (bb_begins.contains(instruction)) {
				bb_list.add(instruction);
			}
		}

		return bb_list;
	}
}
