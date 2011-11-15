package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class ExceptionHandlerMarker extends AbstractMarker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();

		CtrlFlowGraph cfg = new CtrlFlowGraph(method);

		cfg.visit(method.instructions.getFirst());

		for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
			
			List<AbstractInsnNode> exits = cfg.visit(tcb.handler);
			regions.add(new MarkedRegion(AsmHelper.skipVirualInsns(
					tcb.handler, true), exits));
		}

		return regions;
	}

}
