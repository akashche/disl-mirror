package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class BodyMarker implements Marker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		MarkedRegion region = 
			new MarkedRegion(method, method.instructions.getFirst());

		for (AbstractInsnNode instr : method.instructions.toArray()) {
			
			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
				region.addExitPoint(instr);
			}
		}

		regions.add(region);
		return regions;
	}

}
