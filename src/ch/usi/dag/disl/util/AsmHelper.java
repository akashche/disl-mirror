package ch.usi.dag.disl.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.exception.DiSLFatalException;

public class AsmHelper {

	public static boolean before(AbstractInsnNode first, AbstractInsnNode second) {

		while (first != null) {
			first = first.getNext();

			if (first == second) {
				return true;
			}
		}

		return false;
	}

	public static boolean offsetBefore(InsnList ilst, int from, int to) {

		if (from >= to) {
			return false;
		}

		for (int i = from; i < to; i++) {

			if (ilst.get(i).getOpcode() != -1) {
				return true;
			}
		}

		return false;
	}

	public static AbstractInsnNode loadConst(Object var) {

		if (var instanceof Boolean) {

			return new InsnNode((Boolean) var ? Opcodes.ICONST_1
					: Opcodes.ICONST_0);
		} else if (var instanceof Integer || var instanceof Short
				|| var instanceof Byte) {

			int intValue = 0;

			if (var instanceof Integer) {
				intValue = ((Integer) var).intValue();
			} else if (var instanceof Short) {
				intValue = ((Short) var).intValue();
			} else if (var instanceof Byte) {
				intValue = ((Byte) var).intValue();
			}

			switch (intValue) {
			case -1:
				return new InsnNode(Opcodes.ICONST_M1);
			case 0:
				return new InsnNode(Opcodes.ICONST_0);
			case 1:
				return new InsnNode(Opcodes.ICONST_1);
			case 2:
				return new InsnNode(Opcodes.ICONST_2);
			case 3:
				return new InsnNode(Opcodes.ICONST_3);
			case 4:
				return new InsnNode(Opcodes.ICONST_4);
			case 5:
				return new InsnNode(Opcodes.ICONST_5);
			default:
				if (intValue >= -128 && intValue < 128) {
					return new IntInsnNode(Opcodes.BIPUSH, intValue);
				}
			}
		} else if (var instanceof Long) {

			long longValue = ((Long) var).longValue();

			if (longValue == 0) {
				return new InsnNode(Opcodes.LCONST_0);
			} else if (longValue == 1) {
				return new InsnNode(Opcodes.LCONST_1);
			}
		} else if (var instanceof Float) {

			float floatValue = ((Float) var).floatValue();

			if (floatValue == 0) {
				return new InsnNode(Opcodes.FCONST_0);
			} else if (floatValue == 1) {
				return new InsnNode(Opcodes.FCONST_1);
			} else if (floatValue == 2) {
				return new InsnNode(Opcodes.FCONST_2);
			}
		} else if (var instanceof Double) {

			double doubleValue = ((Double) var).doubleValue();

			if (doubleValue == 0) {
				return new InsnNode(Opcodes.DCONST_0);
			} else if (doubleValue == 1) {
				return new InsnNode(Opcodes.DCONST_1);
			}
		}

		return new LdcInsnNode(var);
	}

	public static int getIConstOperand(AbstractInsnNode instr) {

		switch (instr.getOpcode()) {
		case Opcodes.ICONST_M1:
			return -1;
		case Opcodes.ICONST_0:
			return 0;
		case Opcodes.ICONST_1:
			return 1;
		case Opcodes.ICONST_2:
			return 2;
		case Opcodes.ICONST_3:
			return 3;
		case Opcodes.ICONST_4:
			return 4;
		case Opcodes.ICONST_5:
			return 5;
		case Opcodes.BIPUSH:
			return ((IntInsnNode) instr).operand;
		case Opcodes.LDC: {
			LdcInsnNode ldc = (LdcInsnNode) instr;

			if (ldc.cst instanceof Integer) {
				return ((Integer) ldc.cst).intValue();
			}
		}
		default:
			throw new DiSLFatalException("Unknown integer instruction");
		}
	}

	public static int getInternalParamIndex(MethodNode method, int parIndex) {

		Type[] types = Type.getArgumentTypes(method.desc);

		if (parIndex >= types.length) {
			throw new DiSLFatalException("Parameter index out of bound");
		}

		int index = 0;

		for (int i = 0; i < parIndex; i++) {

			// add number of occupied slots
			index += types[i].getSize();
		}

		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			index += 1;
		}

		return index;
	}

	/**
	 * Returns type if the int.class or String.class construct is used
	 */
	public static Type getClassType(AbstractInsnNode instr) {

		switch (instr.getOpcode()) {
		// type for basic class types int, float,...
		case Opcodes.GETSTATIC:

			String owner = ((FieldInsnNode) instr).owner;

			if (owner.endsWith("Boolean")) {
				return Type.BOOLEAN_TYPE;
			} else if (owner.endsWith("Byte")) {
				return Type.BYTE_TYPE;
			} else if (owner.endsWith("Character")) {
				return Type.CHAR_TYPE;
			} else if (owner.endsWith("Double")) {
				return Type.DOUBLE_TYPE;
			} else if (owner.endsWith("Float")) {
				return Type.FLOAT_TYPE;
			} else if (owner.endsWith("Integer")) {
				return Type.INT_TYPE;
			} else if (owner.endsWith("Long")) {
				return Type.LONG_TYPE;
			} else if (owner.endsWith("Short")) {
				return Type.SHORT_TYPE;
			}

			return null;

		// type for object types String,...
		case Opcodes.LDC:

			Object tObj = ((LdcInsnNode) instr).cst;

			if (tObj instanceof Type) {
				return (Type) tObj;
			}
			
			return null;

		default:
			return null;
		}

	}

	public static int calcMaxLocal(InsnList ilst) {

		int max = 0;

		for (AbstractInsnNode instr : ilst.toArray()) {

			if (instr instanceof VarInsnNode) {

				VarInsnNode varInstr = (VarInsnNode) instr;

				switch (varInstr.getOpcode()) {
				case Opcodes.LLOAD:
				case Opcodes.DLOAD:
				case Opcodes.LSTORE:
				case Opcodes.DSTORE:

					if ((varInstr.var + 2) > max) {
						max = varInstr.var + 2;
					}

					break;

				default:
					if ((varInstr.var + 1) > max) {
						max = varInstr.var + 1;
					}

					break;
				}
			} else if (instr instanceof IincInsnNode) {

				IincInsnNode iinc = (IincInsnNode) instr;

				if ((iinc.var + 1) > max) {
					max = iinc.var + 1;
				}
			}
		}

		return max;
	}

	public static void replaceRetWithGoto(InsnList ilst) {

		// collect all returns to the list
		List<AbstractInsnNode> returns = new LinkedList<AbstractInsnNode>();

		for (AbstractInsnNode instr : ilst.toArray()) {
			int opcode = instr.getOpcode();

			if (isReturn(opcode)) {
				returns.add(instr);
			}
		}

		if (returns.size() > 1) {
			
			// replace 'return' instructions with 'goto' that will point to the
			// end 
			LabelNode endL = new LabelNode(new Label());
			ilst.add(endL);

			for (AbstractInsnNode instr : returns) {
				ilst.insertBefore(instr, new JumpInsnNode(Opcodes.GOTO, endL));
				ilst.remove(instr);
			}
			
		} else if (returns.size() == 1) {
			
			// there is only one return at the end
			ilst.remove(returns.get(0));
		}
	}

	// makes a clone of an instruction list
	public static InsnList cloneInsnList(InsnList src) {

		Map<LabelNode, LabelNode> map = createLabelMap(src);
		return cloneInsnList(src, map);
	}

	public static Map<LabelNode, LabelNode> createLabelMap(InsnList src) {

		Map<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>();

		// iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : src.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = new LabelNode(new Label());
				map.put((LabelNode) instr, label);
			}
		}

		return map;
	}

	public static InsnList cloneInsnList(InsnList src,
			Map<LabelNode, LabelNode> map) {

		InsnList dst = new InsnList();

		// copy instructions using clone
		AbstractInsnNode instr = src.getFirst();
		while (instr != src.getLast().getNext()) {

			// special case where we put a new label instead of old one
			if (instr instanceof LabelNode) {

				dst.add(map.get(instr));

				instr = instr.getNext();

				continue;
			}

			dst.add(instr.clone(map));

			instr = instr.getNext();
		}

		return dst;
	}

	public static List<TryCatchBlockNode> cloneTryCatchBlocks(
			List<TryCatchBlockNode> src, Map<LabelNode, LabelNode> map) {

		List<TryCatchBlockNode> dst = new LinkedList<TryCatchBlockNode>();

		for (TryCatchBlockNode tcb : src) {

			dst.add(new TryCatchBlockNode(map.get(tcb.start), map.get(tcb.end),
					map.get(tcb.handler), tcb.type));
		}

		return dst;
	}

	public static AbstractInsnNode skipVirualInsns(AbstractInsnNode instr,
			boolean isForward) {
		
		while (instr != null && isVirtualInstr(instr)) {
			instr = isForward ? instr.getNext() : instr.getPrevious();
		}

		return instr;
	}

	public static boolean isVirtualInstr(AbstractInsnNode instr) {
		
		return instr.getOpcode() == -1;
	}
	
	// detects if the instruction list contains only return
	public static boolean containsOnlyReturn(InsnList ilst) {

		AbstractInsnNode instr = ilst.getFirst();
		
		while(instr != null && isVirtualInstr(instr)) {
			instr = instr.getNext();
		}
		
		return isReturn(instr.getOpcode());
	}

	public static boolean isReturn(int opcode) {
		return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
	}

	public static boolean isBranch(AbstractInsnNode instruction) {

		int opcode = instruction.getOpcode();

		return instruction instanceof JumpInsnNode
				|| instruction instanceof LookupSwitchInsnNode
				|| instruction instanceof TableSwitchInsnNode
				|| opcode == Opcodes.ATHROW || opcode == Opcodes.RET
				|| (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
	}

	public static boolean isConditionalBranch(AbstractInsnNode instruction) {

		int opcode = instruction.getOpcode();

		return (instruction instanceof JumpInsnNode && opcode != Opcodes.GOTO);
	}

	public static boolean mightThrowException(AbstractInsnNode instruction) {

		switch (instruction.getOpcode()) {

		// NullPointerException, ArrayIndexOutOfBoundsException
		case Opcodes.BALOAD:
		case Opcodes.DALOAD:
		case Opcodes.FALOAD:
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.DASTORE:
		case Opcodes.FASTORE:
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.AALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
		case Opcodes.SASTORE:
			// NullPointerException, ArrayIndexOutOfBoundsException,
			// ArrayStoreException
		case Opcodes.AASTORE:
			// NullPointerException
		case Opcodes.ARRAYLENGTH:
		case Opcodes.ATHROW:
		case Opcodes.GETFIELD:
		case Opcodes.PUTFIELD:
			// NullPointerException, StackOverflowError
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKEVIRTUAL:
			// StackOverflowError
		case Opcodes.INVOKESTATIC:
			// NegativeArraySizeException
		case Opcodes.ANEWARRAY:
			// NegativeArraySizeException, OutOfMemoryError
		case Opcodes.NEWARRAY:
		case Opcodes.MULTIANEWARRAY:
			// OutOfMemoryError, InstantiationError
		case Opcodes.NEW:
			// OutOfMemoryError
		case Opcodes.LDC:
			// ClassCastException
		case Opcodes.CHECKCAST:
			// ArithmeticException
		case Opcodes.IDIV:
		case Opcodes.IREM:
		case Opcodes.LDIV:
		case Opcodes.LREM:
			// New instruction in JDK7
		case Opcodes.INVOKEDYNAMIC:
			return true;
		default:
			return false;
		}
	}
	
	// hepler method for boxValueOnStack
	private static MethodInsnNode constructValueOf(Class<?> bigClass,
			Class<?> smallClass) {
		
		Type bigType = Type.getType(bigClass);
		return new MethodInsnNode(Opcodes.INVOKESTATIC,
				// converting class name
				bigType.getInternalName(),
				// converting method descriptor
				"valueOf",
				"(" + Type.getDescriptor(smallClass) + ")"
				+ bigType.getDescriptor());
	}
	
	/**
	 * Returns instruction that will call the method to box the instruction
	 * residing on the stack
	 * 
	 * @param typeToBox type to be boxed
	 */
	public static AbstractInsnNode boxValueOnStack(Type typeToBox) {
		
		switch(typeToBox.getSort()) {

		case Type.BOOLEAN:
			return constructValueOf(Boolean.class, boolean.class);
		case Type.BYTE:
			return constructValueOf(Byte.class, byte.class);
		case Type.CHAR:
			return constructValueOf(Character.class, char.class);
		case Type.DOUBLE:
			return constructValueOf(Double.class, double.class);
		case Type.FLOAT:
			return constructValueOf(Float.class, float.class);
		case Type.INT:
			return constructValueOf(Integer.class, int.class);
		case Type.LONG:
			return constructValueOf(Long.class, long.class);
		case Type.SHORT:
			return constructValueOf(Short.class, short.class);
		
		default:
			throw new DiSLFatalException("Cannot box type: "
					+ typeToBox.getDescriptor());
		}
	}
	
	// TODO ! processor - move method to weaver
	public static InsnList createGetArgsCode(String methodDescriptor) {
		
		InsnList insnList = new InsnList();
		
		Type[] argTypes = Type.getArgumentTypes(methodDescriptor);
		
		// array creation code (length is the length of arguments)
		insnList.add(AsmHelper.loadConst(argTypes.length));
		insnList.add(new InsnNode(Opcodes.ANEWARRAY));
		
		int argIndex = 0;
		for(int i = 0; i < argTypes.length; ++i) {
			
			// ** add new array store **

			// duplicate array object
			insnList.add(new InsnNode(Opcodes.DUP));
			
			// add index into the array where to store the value
			insnList.add(AsmHelper.loadConst(i));
			
			Type argType = argTypes[i];
			
			// load "object" that will be stored
			int loadOpcode = argType.getOpcode(Opcodes.ILOAD);
			insnList.add(new VarInsnNode(loadOpcode, argIndex));
			
			// box non-reference type
			if(! (argType.getSort() == Type.OBJECT
					|| argType.getSort() == Type.ARRAY) ) {
				insnList.add(boxValueOnStack(argType));
			}
			
			// store the value into the array on particular index
			insnList.add(new InsnNode(Opcodes.AASTORE));
			
			// shift argument index according to argument size
			argIndex += argType.getSize();
		}
		
		return insnList;
	}
}
