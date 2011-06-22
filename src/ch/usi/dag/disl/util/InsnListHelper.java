package ch.usi.dag.disl.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
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
		// Similar to 'const Node **instr' in c. 
		final AbstractInsnNode instr[] = new AbstractInsnNode[1];
		instr[0] = method.instructions.getFirst();
		// TODO null has passed to construct this method node. Untested.
		MethodNode temp = new MethodNode(method.access, method.name,
				method.desc, method.signature, null);

		// WARNNING When a method node is going to visit another method node,
		// directly or indirectly, it will trigger the rebuilding of the 
		// instruction list. And that is what this method depends on.
		method.accept(new AdviceAdapter(temp, method.access, method.name,
				method.desc) {
			@Override
			public void onMethodEnter() {
				// Now the pointer has pointed to the last instruction
				// of the object initialization. Generally speaking, it's
				// an invoke special instruction that calls other constructor
				// of this class or the constructor of the super class.
				instr[0] = ((MethodNode) mv).instructions.getLast();
			}
		});
		
		if (instr[0]==null){
			// This is not a constructor. Just return the first instruction  
			return method.instructions.getFirst();
		}else{
			// Now the rebuilding has done. And what we need is the instruction 
			// right after the object initialization.
			return instr[0].getNext();
		}
	}
}
