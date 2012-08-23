package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;

public class TryClauseMarker extends AbstractDWRMarker {

	@Override
	public List<MarkedRegion> markWithDefaultWeavingReg(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();

		for (TryCatchBlockNode tcb : method.tryCatchBlocks) {

			AbstractInsnNode start = AsmHelper.skipVirualInsns(tcb.start, true);
			AbstractInsnNode end = AsmHelper.skipVirualInsns(tcb.end, false);
			regions.add(new MarkedRegion(start, end));
		}

		return regions;
	}

}
