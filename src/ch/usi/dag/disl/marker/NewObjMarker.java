package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.util.Constants;

public class NewObjMarker implements Marker {

	protected boolean isPrecise = false;

	// NOTE: does not work for arrays
	
	@Override
	public List<MarkedRegion> mark(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		InsnList ilst = method.instructions;

		int invokedNews = 0;
		
		// find invocation of constructor after new instruction
		for (AbstractInsnNode instruction : ilst.toArray()) {
			
			// track new instruction
			if(instruction.getOpcode() == Opcodes.NEW) {
				
				++invokedNews;
			}
			
			// if it is invoke special and there are new pending
			if(instruction.getOpcode() == Opcodes.INVOKESPECIAL
					&& invokedNews > 0) {
				
				MethodInsnNode min = (MethodInsnNode) instruction;
				
				if(min.name.equals(Constants.CONSTRUCTOR_NAME)) {
					
					regions.add(
							new MarkedRegion(method, instruction, instruction));
				}
			}
		}

		return regions;
	}

}
