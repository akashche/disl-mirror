package ch.usi.dag.disl.weaver;

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

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// TODO include analysis
	// TODO support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings) {

		for (Snippet snippet : snippetMarkings.keySet()) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			InsnList ilst = snippet.getAsmCode();

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
					ilst.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO,
							label));
					ilst.remove(instr);
				}
			}
			else if (returns.size() == 1) {
				ilst.remove(returns.get(0));
			}

			// Instrument
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {
					region.ilst.insertBefore(region.start, ilst);
				}
			}
			else if (snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.ends) {
						region.ilst.insert(exit, ilst);
					}
				}
			}
		}
	}
}
