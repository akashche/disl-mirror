package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.weaver.Weaver;

// partial and hacked implementation of processor
public class ProcessorHack {

	public @interface Processor {
		Class<?> type();
	}
	
	protected static Map<Type, InsnList> processors = 
		new HashMap<Type, InsnList>();

	static class ArgData {
		int pos;
		int offset;
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
		
		if (data.asmCode != null) {
			list.add(data);
		}
		
		int offset = 0;
		
		// For each argument
		for (int i=0; i<types.length; i++){
			data = new ArgData();
			data.pos = i;
			data.offset = offset;
			data.allCount = types.length;
			data.isStatic = isStatic;
			data.type = types[i];
			data.asmCode = processors.get(data.type);

			if (data.type.equals(Type.DOUBLE_TYPE)
					|| data.type.equals(Type.LONG_TYPE)) {
				offset++;
			}
			
			offset++;

			if (data.asmCode != null) {
				list.add(data);
			}
		}
		
		return list;
	}
	
	public static Object pseudoVar(ClassNode clazz, MethodNode method, 
			String owner, String name){
		
		if (owner.equals("ch/usi/dag/disl/staticinfo/analysis/StaticContext")){
			
			if (name.equals("getFullName")){
				return clazz.name + "." + method.name;
			}else if (name.equals("getClassName")){
				return clazz.name;
			}else if (name.equals("getMethodName")){
				return method.name;
			}
		}
		
		return null;
	}

	public static void instrument(ClassNode clazz, MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocals) {

		List<ArgData> instrData = createArgData(methodNode);

		AbstractInsnNode first = methodNode.instructions.getFirst();
		int max = methodNode.maxLocals;

		for (ArgData data : instrData) {
			InsnList ilst = AsmHelper.cloneList(data.asmCode);
			AsmHelper.removeReturns(ilst);
			AbstractInsnNode temp = null;

			for (AbstractInsnNode instr : ilst.toArray()) {
				int opcode = instr.getOpcode();
				
				if (opcode == -1)
					continue;

				switch (opcode) {
				case Opcodes.ILOAD:
				case Opcodes.LLOAD:
				case Opcodes.FLOAD:
				case Opcodes.DLOAD:
				case Opcodes.ALOAD:

					VarInsnNode varInstr = (VarInsnNode) instr;
					
					if (data.allCount == -1){						
						break;
					}

					switch (varInstr.var) {
					// Index of the argument
					case 0:
						temp = new LdcInsnNode(data.pos);
						ilst.insertBefore(instr, temp);
						ilst.remove(instr);
						continue;

					// Number of the arguments
					case 1:
						temp = new LdcInsnNode(data.allCount);
						ilst.insertBefore(instr, temp);
						ilst.remove(instr);
						continue;

					// Value
					case 2:
						varInstr.var = data.offset + (data.isStatic ? 0 : 1);
						continue;

					default:						
						break;
					}

					break;
					

				case Opcodes.ISTORE:
				case Opcodes.LSTORE:
				case Opcodes.FSTORE:
				case Opcodes.DSTORE:
				case Opcodes.ASTORE:
					break;

				case Opcodes.INVOKEVIRTUAL:
					
					AbstractInsnNode previous = instr.getPrevious();
					
					if (previous == null || 
							previous.getOpcode() != Opcodes.INVOKESPECIAL) {
						break;
					}
					
					previous = previous.getPrevious();
					
					if (previous == null || previous.getOpcode() != Opcodes.DUP) {
						break;
					}

					previous = previous.getPrevious();
					
					if (previous == null || previous.getOpcode() != Opcodes.NEW) {
						break;
					}
					
					MethodInsnNode invocation = (MethodInsnNode) instr;
					
					Object const_var = pseudoVar(clazz, methodNode, 
							invocation.owner, invocation.name);

					if (const_var != null) {
						// Insert a ldc instruction and remove the pseudo ones.
						ilst.insert(instr, new LdcInsnNode(const_var));
						
						while (previous != instr){
							AbstractInsnNode temp_instr = previous;
							previous = previous.getNext();
							ilst.remove(temp_instr);
						}
						
						ilst.remove(instr);
					}
					
					break;
					
				default:
					break;
				}
				
				if (instr instanceof VarInsnNode){
					VarInsnNode varInstr = (VarInsnNode) instr;
					
					varInstr.var += methodNode.maxLocals;

					if (varInstr.var > max) {
						max = varInstr.var;
					}
				}
			}
			
			methodNode.instructions.insertBefore(first, ilst);
		}

		methodNode.maxLocals = max;
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
