package ch.usi.dag.disl.util.cfg;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

public class Detector {

	public static boolean containsCatch(MethodNode method) {

		if (method.tryCatchBlocks.size() == 0) {
			return false;
		}

		CtrlFlowGraph cfg = new CtrlFlowGraph(method);
		cfg.visit(method.instructions.getFirst());

		for (int i = method.tryCatchBlocks.size() - 1; i >= 0; i--) {

			TryCatchBlockNode tcb = method.tryCatchBlocks.get(i);

			if (cfg.visit(tcb.handler).size() != 0) {
				return true;
			}
		}

		return false;
	}
}
