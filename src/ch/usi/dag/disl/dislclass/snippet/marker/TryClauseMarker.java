package ch.usi.dag.disl.dislclass.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;

public class TryClauseMarker implements Marker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		
		for (TryCatchBlockNode tcb : method.tryCatchBlocks){
			AbstractInsnNode start = AsmHelper.skipVirualInsns(tcb.start, true);
			AbstractInsnNode end = AsmHelper.skipVirualInsns(tcb.end, false);
			
			regions.add(new MarkedRegion(method, start, end));
		}
		
		return regions;
	}

}
