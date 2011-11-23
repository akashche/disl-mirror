package ch.usi.dag.disl.example.map;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class MonitorSC extends AbstractStaticContext {

	public boolean hasMonitor() {

		InsnList inslist = staticContextData.getMethodNode().instructions;

		Iterator<AbstractInsnNode> it = inslist.iterator();
		while (it.hasNext()) {
			if (it.next().getOpcode() == Opcodes.MONITORENTER)
				return true;
		}
		return false;
	}
}
