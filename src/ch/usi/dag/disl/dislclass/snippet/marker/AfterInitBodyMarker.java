package ch.usi.dag.disl.dislclass.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.Constants;

public class AfterInitBodyMarker implements Marker {

	// TODO ! not marking correctly
	
	// empty visitor for new AdviceAdapter
	private static class EmptyMethodVisitor extends MethodVisitor {

		public EmptyMethodVisitor() {
			super(Opcodes.ASM4);
		}
	}
	
	// Get the first valid mark of a method.
	// For a constructor, the return value will be the instruction after
	// the object initialization.
	public AbstractInsnNode findFirstValidMark(MethodNode method) {
		
		AbstractInsnNode first = method.instructions.getFirst();

		// This is not a constructor. Just return the first instruction
		if (!method.name.equals(Constants.CONSTRUCTOR_NAME)) {
			return first;
		}

		// AdviceAdapter will help us with identifying the proper place where
		// the constructor to super is called 
		
		// just need an object that will hold a value
		//  - we need access to the changeable boolean via reference
		class DataHolder {
			boolean trigger = false;
		}
		final DataHolder dh = new DataHolder();

		AdviceAdapter adapter = new AdviceAdapter(Opcodes.ASM4,
				new EmptyMethodVisitor(), method.access, method.name,
				method.desc) {

			public void onMethodEnter() {
				dh.trigger = true;
			}
		};

		// Iterate instruction list till the instruction right after the
		// object initialization
		adapter.visitCode();

		for (AbstractInsnNode iterator : method.instructions.toArray()) {
			
			iterator.accept(adapter);

			// first instruction will be instruction after constructor call
			if (dh.trigger) {
				first = iterator.getNext();
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
