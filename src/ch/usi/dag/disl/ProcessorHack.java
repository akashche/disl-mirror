package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;

// partial and hacked implementation of processor
public class ProcessorHack {

	public @interface Processor {
		String type();
	}
	
	protected static Map<Type, InsnList> processors = 
		new HashMap<Type, InsnList>();

	class ArgData {
		int pos;
		int allCount;
		boolean isStatic;
		InsnList asmCode;
		Type type;
		
	}
	
	public static void parseProcessor(String className, MethodNode method) {
		
		/*
		if (method.invisibleAnnotations == null) {
			throw new RuntimeException("DiSL anottation for method "
					+ method.name + " is missing");
		}

		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			throw new RuntimeException("DiSL method " + method.name
					+ " should be declared as static");
		}

		// get first annotation and its first param value - all in one :)
		List<?> annotValues = ((AnnotationNode) method.invisibleAnnotations.get(0)).values;
		Type processorType = (Type) annotValues.get(1);
		
		processors.put(processorType, method.instructions);
		*/
		
		processors.put((Type) ((AnnotationNode) method.invisibleAnnotations.get(0)).values.get(1), method.instructions);
	}
	
	private static List<ArgData> createArgData(MethodNode methodNode) {

		return null;
	}
	
	public static void instrument(MethodNode methodNode, List<SyntheticLocalVar> syntheticLocals) {
		
		List<ArgData> instrData = createArgData(methodNode);
	}

	/* use case

	@SyntheticLocal
	static String flag;

	@ProcessorHack.Processor(type=void.class)
	public static void processor1 () {
		// this will be called before any processor
		
		flag = "0";
	}
	
	@ProcessorHack.Processor(type=Object.class)
	public static void processor2 (int a, int b, Object c) {
		
		flag = "1";
	}
	
	@ProcessorHack.Processor(type=int.class)
	public static void processor3 (int a, int b, int c) {
		
		flag = "2";
	}
	*/
}
