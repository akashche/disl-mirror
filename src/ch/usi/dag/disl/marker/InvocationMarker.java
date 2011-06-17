package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InvocationMarker implements Marker {

	@Override
	public List<MarkRegion> mark(MethodNode method) {
		List<MarkRegion> regions = new LinkedList<MarkRegion>();
		InsnList ilst = method.instructions;

		for (AbstractInsnNode instruction : ilst.toArray())
			if (instruction instanceof MethodInsnNode)
				regions.add(new MarkRegion(instruction, instruction));

		return regions;
	}
}
