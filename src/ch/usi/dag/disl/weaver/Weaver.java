package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	public void removeReturns(InsnList ilst) {
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
	public InsnList cloneList(InsnList src) {
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

	public int fixLocalIndex(int maxLocals, InsnList src) {
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

	// TODO include analysis
	// TODO support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings) {
		// Sort the snippets based on their order
		ArrayList<Snippet> array = new ArrayList<Snippet>(snippetMarkings.keySet());
		Collections.sort(array);

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			InsnList ilst = snippet.getAsmCode();

			removeReturns(ilst);

			// Instrument
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {
					InsnList newlst = cloneList(ilst);
					fixLocalIndex(region.methodnode.maxLocals, newlst);
					region.methodnode.instructions.insertBefore(region.start,
							newlst);
				}
			} else if (snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.ends) {
						InsnList newlst = cloneList(ilst);
						fixLocalIndex(region.methodnode.maxLocals, newlst);
						region.methodnode.instructions.insert(exit, newlst);
					}
				}
			}
		}
	}

}
