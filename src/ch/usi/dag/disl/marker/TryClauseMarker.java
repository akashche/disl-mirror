package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;

/**
 * Marks try block.
 * <br>
 * <br>
 * Sets the start at the beginning of a try block and the end at the end of a
 * try block.
 */
public class TryClauseMarker extends AbstractDWRMarker {

	@Override
	public List<MarkedRegion> markWithDefaultWeavingReg(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();

		for (TryCatchBlockNode tcb : method.tryCatchBlocks) {

			AbstractInsnNode start = AsmHelper.skipVirtualInsns(tcb.start, true);
			AbstractInsnNode end = AsmHelper.skipVirtualInsns(tcb.end, false);
			regions.add(new MarkedRegion(start, end));
		}

		return regions;
	}

}
