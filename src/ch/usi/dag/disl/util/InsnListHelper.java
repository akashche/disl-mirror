package ch.usi.dag.disl.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class InsnListHelper {

	public static void removeReturns(InsnList ilst) {
		// Remove 'return' instruction
		List<AbstractInsnNode> returns = new LinkedList<AbstractInsnNode>();

		for (AbstractInsnNode instr : ilst.toArray()) {
			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
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
		Map<AbstractInsnNode, AbstractInsnNode> map = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		InsnList dst = new InsnList();

		// First iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : src.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = new LabelNode(new Label());
				map.put(instr, label);
			}
		}

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr instanceof LabelNode) {
				dst.add(map.get(instr));
				continue;
			}

			dst.add(instr.clone(map));
		}

		return dst;
	}

	public static int fixLocalIndex(int maxLocals, InsnList src) {
		int max = maxLocals;

		for (AbstractInsnNode instr : src.toArray()) {

			if (instr instanceof VarInsnNode) {
				VarInsnNode varInstr = (VarInsnNode) instr;
				varInstr.var += maxLocals;

				if (varInstr.var > max) {
					max = varInstr.var;
				}
			}
		}

		return max;
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
		
		AbstractInsnNode instr = ilst.getFirst();
		
		int opcode = instr.getOpcode();
		
		if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
			return true;
		}
		
		return false;
	}
}
