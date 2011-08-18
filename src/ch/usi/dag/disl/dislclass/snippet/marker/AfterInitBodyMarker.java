package ch.usi.dag.disl.dislclass.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.Constants;

public class AfterInitBodyMarker implements Marker {

	// Get the first valid mark of a method.
	// For a constructor, the return value will be the instruction after
	// the object initialization.
	public AbstractInsnNode findFirstValidMark(MethodNode method) {
		
		AbstractInsnNode first = method.instructions.getFirst();

		// This is not a constructor. Just return the first instruction
		if (!method.name.equals(Constants.CONSTRUCTOR_NAME)) {
			return first;
		}

		// Similar to 'const boolean **trigger' in c.
		final boolean trigger[] = { false };

		AdviceAdapter adapter = new AdviceAdapter(new EmptyVisitor(),
				method.access, method.name, method.desc) {

			public void onMethodEnter() {
				trigger[0] = true;
			}
		};

		// Iterate instruction list till the instruction right after the
		// object initialization
		adapter.visitCode();

		for (AbstractInsnNode iterator : method.instructions.toArray()) {
			
			iterator.accept(adapter);

			if (trigger[0]) {
				first = iterator.getPrevious();
				break;
			}
		}

		return first;
	}
	
	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		
		MarkedRegion region = 
			new MarkedRegion(method, findFirstValidMark(method));

		for (AbstractInsnNode instr : method.instructions.toArray()) {
			
			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
				region.addExitPoint(instr.getPrevious());
			}
		}

		regions.add(region);
		return regions;
	}

}
