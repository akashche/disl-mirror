package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;
import ch.usi.dag.disl.weaver.Weaver;

// partial and hacked implementation of processor
public class ProcessorHack {

	public @interface Processor {
		String type();
	}
	
	protected static Map<Type, InsnList> processors = 
		new HashMap<Type, InsnList>();

	static class ArgData {
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
		List<ArgData> list = new LinkedList<ArgData>();
		Type[] types = Type.getArgumentTypes(methodNode.desc);
		boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC)>0;
		
		ArgData data = null;
		
		// The first processor
		data = new ArgData();
		data.pos = -1;
		data.allCount = -1;
		data.isStatic = isStatic;
		data.type = Type.VOID_TYPE;
		data.asmCode = processors.get(data.type);
		list.add(data);
		
		// For each argument
		for (int i=0; i<types.length; i++){
			data = new ArgData();
			data.pos = i;
			data.allCount = types.length;
			data.isStatic = isStatic;
			data.type = types[i];
			data.asmCode = processors.get(data.type);
			list.add(data);
		}
		
		return list;
	}

	public static void instrument(MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocals) {

		List<ArgData> instrData = createArgData(methodNode);

		AbstractInsnNode first = methodNode.instructions.getFirst();

		for (ArgData data : instrData) {
			InsnList ilst = InsnListHelper.cloneList(data.asmCode);
			AbstractInsnNode temp = null;
			
			if (data.allCount == -1){
				methodNode.instructions.insertBefore(first, ilst);
				continue;
			}

			for (AbstractInsnNode instr : ilst.toArray()) {
				int opcode = instr.getOpcode();

				switch (opcode) {
				case Opcodes.ILOAD:
				case Opcodes.LLOAD:
				case Opcodes.FLOAD:
				case Opcodes.DLOAD:
				case Opcodes.ALOAD:

					VarInsnNode varInstr = (VarInsnNode) instr;

					switch (varInstr.var) {
					// Index of the argument
					case 0:
						temp = new LdcInsnNode(data.pos);
						ilst.insertBefore(instr, temp);
						ilst.remove(instr);
						break;

					// Number of the arguments
					case 1:
						temp = new LdcInsnNode(data.allCount);
						ilst.insertBefore(instr, temp);
						ilst.remove(instr);
						break;

					// Value
					case 2:
						varInstr.var = data.pos + (data.isStatic ? 0 : 1);
						break;

					default:
						break;
					}

					break;

				default:
					break;
				}
			}

			methodNode.instructions.insertBefore(first, ilst);
		}

		Weaver.static2Local(methodNode, syntheticLocals);
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
