package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// If 'end' is the previous instruction of 'start', then the basic block
	// contains only one branch instruction.
	public int isPrevious(AbstractInsnNode end, AbstractInsnNode start) {
		// This happens only when the first instruction is a branch instruction.
		if (end == null)
			return 0;

		// Skip labels and compare the first not-label one with 'end'.
		AbstractInsnNode iterator = start.getPrevious();
		int label_count = 0;

		while (iterator != null) {
			if (iterator.getOpcode() == -1) {
				iterator = iterator.getPrevious();
				label_count++;
			} else {
				return iterator == end ? label_count : -1;
			}
		}

		return -1;
	}

	// TODO analysis: include analysis
	// TODO ! support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars) {
		// Sort the snippets based on their order
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		// Fix empty region
		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);

			for (MarkedRegion region : regions) {
				// For iterating through the list. NOTE that we are going to
				// remove/add new items into the list.
				AbstractInsnNode ends[] = new AbstractInsnNode[region.getEnds()
						.size()];
				
				for (AbstractInsnNode exit : region.getEnds().toArray(ends)) {
					AbstractInsnNode start = region.getStart();

					switch (isPrevious(exit, start)) {
					case -1:
						break;
					case 0:
						// No label? Then give them one label!
						region.getMethodnode().instructions.insertBefore(start,
								new LabelNode(new Label()));
					default:
						// Now we have a label between them. Both 'start' and
						// 'end' will set to the label node.
						region.setStart(region.getStart().getPrevious());
						region.getEnds().remove(exit);
						region.addExitPoint(region.getStart());
						break;
					}
				}
			}
		}

		Set<MethodNode> instrumented_methods = new HashSet<MethodNode>();

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			InsnList ilst = snippet.getAsmCode();

			// skip snippets with empty code
			if (snippet.getAsmCode() == null) {
				continue;
			}

			// Instrument
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {
					InsnList newlst = InsnListHelper.cloneList(ilst);
					InsnListHelper
							.fixLocalIndex(region.getMethodnode(), newlst);
					region.getMethodnode().instructions.insertBefore(
							region.getStart(), newlst);
					instrumented_methods.add(region.getMethodnode());
				}
			} else if (snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.getEnds()) {
						InsnList newlst = InsnListHelper.cloneList(ilst);
						InsnListHelper.fixLocalIndex(region.getMethodnode(),
								newlst);
						region.getMethodnode().instructions
								.insert(exit, newlst);
						instrumented_methods.add(region.getMethodnode());
					}
				}
			}
		}

		// Transform static fields to synthetic local
		for (MethodNode method : instrumented_methods) {
			InsnListHelper.static2Local(method, syntheticLocalVars);
		}

	}
}
