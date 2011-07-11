package ch.usi.dag.disl;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;

// Partial working but hacked implementation of processor
public class ProcessorHack {

	class Processor {
		
		// TODO type
		InsnList asmCode;
	}
	
	protected static List<ArgData> processors = 
		new LinkedList<ArgData>();

	class ArgData {
		int pos;
		int allCount;
		boolean isStatic;
		InsnList asmCode;
		
	}
	
	public static void parseProcessor(String className, MethodNode method) {
		
	}
	
	private static List<ArgData> createArgData(MethodNode methodNode) {

		return null;
	}
	
	public static void instrument(MethodNode methodNode, List<SyntheticLocalVar> syntheticLocals) {
		
		List<ArgData> instrData = createArgData(methodNode);
		
	}
}
