package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// TODO analysis: include analysis
	// TODO ! support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars) {
		// Sort the snippets based on their order
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

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
