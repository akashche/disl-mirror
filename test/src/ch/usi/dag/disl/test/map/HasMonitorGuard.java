package ch.usi.dag.disl.test.map;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.guard.SnippetGuard;

//check if the method has at least one monitorenter on its body.
public class HasMonitorGuard implements SnippetGuard {
	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		InsnList inslist = methodNode.instructions;
		
		Iterator<AbstractInsnNode> it=inslist.iterator();
		while(it.hasNext()) {
			if(it.next().getOpcode() == Opcodes.MONITORENTER)
				return true;
		}
		return false;
		
	}
}
