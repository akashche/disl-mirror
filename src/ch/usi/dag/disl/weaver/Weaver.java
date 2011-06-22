package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// TODO include analysis
	// TODO support for synthetic local
	public void instrument(ClassNode classNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLoclaVars) {
		// Sort the snippets based on their order
		ArrayList<Snippet> array = new ArrayList<Snippet>(
				snippetMarkings.keySet());
		Collections.sort(array);

		for (Snippet snippet : array) {
			List<MarkedRegion> regions = snippetMarkings.get(snippet);
			InsnList ilst = snippet.getAsmCode();

			// TODO remove this line of code
			InsnListHelper.removeReturns(ilst);

			// Instrument
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (MarkedRegion region : regions) {
					InsnList newlst = InsnListHelper.cloneList(ilst);
					InsnListHelper.fixLocalIndex(region.methodnode.maxLocals,
							newlst);
					region.methodnode.instructions.insertBefore(region.start,
							newlst);
				}
			} else if (snippet.getAnnotationClass().equals(After.class)) {
				for (MarkedRegion region : regions) {
					for (AbstractInsnNode exit : region.ends) {
						InsnList newlst = InsnListHelper.cloneList(ilst);
						InsnListHelper.fixLocalIndex(
								region.methodnode.maxLocals, newlst);
						region.methodnode.instructions.insert(exit, newlst);
					}
				}
			}
		}
	}

}
