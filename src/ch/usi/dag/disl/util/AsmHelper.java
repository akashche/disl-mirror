package ch.usi.dag.disl.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
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

public abstract class AsmHelper {

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


	public static AbstractInsnNode loadConst (final Object value) {
		if (value instanceof Boolean) {
			return new InsnNode (
				((Boolean) value) ? Opcodes.ICONST_1 : Opcodes.ICONST_0
			);

		} else if (
			value instanceof Byte ||
			value instanceof Short ||
			value instanceof Integer
		) {
			int intValue = 0;
			if (value instanceof Integer) {
				intValue = ((Integer) value).intValue ();
			} else if (value instanceof Short) {
				intValue = ((Short) value).intValue ();
			} else if (value instanceof Byte) {
				intValue = ((Byte) value).intValue ();
			}

			switch (intValue) {
			case -1:
				return new InsnNode (Opcodes.ICONST_M1);
			case 0:
				return new InsnNode (Opcodes.ICONST_0);
			case 1:
				return new InsnNode (Opcodes.ICONST_1);
			case 2:
				return new InsnNode (Opcodes.ICONST_2);
			case 3:
				return new InsnNode (Opcodes.ICONST_3);
			case 4:
				return new InsnNode (Opcodes.ICONST_4);
			case 5:
				return new InsnNode (Opcodes.ICONST_5);
			default:
				if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
					return new IntInsnNode (Opcodes.BIPUSH, intValue);
				} else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
					return new IntInsnNode (Opcodes.SIPUSH, intValue);
				} else {
					// Make sure LDC argument is an Integer
					return new LdcInsnNode (Integer.valueOf (intValue));
				}
			}

		} else if (value instanceof Long) {
			final long longValue = ((Long) value).longValue ();

			if (longValue == 0) {
				return new InsnNode (Opcodes.LCONST_0);
			} else if (longValue == 1) {
				return new InsnNode (Opcodes.LCONST_1);
			}

			// default to LDC

		} else if (value instanceof Float) {
			final float floatValue = ((Float) value).floatValue ();

			if (floatValue == 0) {
				return new InsnNode (Opcodes.FCONST_0);
			} else if (floatValue == 1) {
				return new InsnNode (Opcodes.FCONST_1);
			} else if (floatValue == 2) {
				return new InsnNode (Opcodes.FCONST_2);
			}

			// default to LDC

		} else if (value instanceof Double) {
			final double doubleValue = ((Double) value).doubleValue ();

			if (doubleValue == 0) {
				return new InsnNode (Opcodes.DCONST_0);
			} else if (doubleValue == 1) {
				return new InsnNode (Opcodes.DCONST_1);
			}

			// default to LDC
		}

		return new LdcInsnNode (value);
	}


	public static String getStringConstOperand (final AbstractInsnNode insn) {
		if (insn.getOpcode () == Opcodes.LDC) {
			final LdcInsnNode ldcNode = (LdcInsnNode) insn;
			if (ldcNode.cst instanceof String) {
				return (String) ldcNode.cst;
			} else {
				throw new DiSLFatalException ("LDC operand is not a String");
			}

		} else {
			throw new DiSLFatalException (String.format (
				"Expected LdcInsnNode, but found %s (%s)",
				insn.getClass ().getSimpleName (), AsmOpcodes.valueOf (insn)
			));
		}
	}


	public static int getIntConstOperand (final AbstractInsnNode insn) {

		switch (insn.getOpcode()) {
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
		case Opcodes.SIPUSH:
			return ((IntInsnNode) insn).operand;
		case Opcodes.LDC: {
			final LdcInsnNode ldc = (LdcInsnNode) insn;
			if (ldc.cst instanceof Integer) {
				return (Integer) ldc.cst;
			} else {
				throw new DiSLFatalException ("LDC operand is not an integer");
			}
		}

		default:
			throw new DiSLFatalException (String.format (
				"Cannot extract integer constant operand from %s (%s)",
				insn.getClass ().getSimpleName (), AsmOpcodes.valueOf (insn)
			));
		}
	}


	public static AbstractInsnNode loadThis () {
		return loadObjectVar (0);
	}


	public static AbstractInsnNode loadObjectVar (final int slot) {
		return loadVar (Type.getType (Object.class), slot);
	}


	public static AbstractInsnNode loadVar (final Type type, final int slot) {
		return new VarInsnNode (type.getOpcode (Opcodes.ILOAD), slot);
	}


	public static AbstractInsnNode storeVar (final Type type, final int slot) {
		return new VarInsnNode (type.getOpcode (Opcodes.ISTORE), slot);
	}


	public static AbstractInsnNode loadNull () {
		return loadDefault (Type.getType (Object.class));
	}


	public static AbstractInsnNode loadDefault (Type type) {
		switch (type.getSort ()) {
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.INT:
		case Type.SHORT:
			return new InsnNode(Opcodes.ICONST_0);
		case Type.LONG:
			return new InsnNode(Opcodes.LCONST_0);
		case Type.FLOAT:
			return new InsnNode(Opcodes.FCONST_0);
		case Type.DOUBLE:
			return new InsnNode(Opcodes.DCONST_0);
		case Type.OBJECT:
			return new InsnNode(Opcodes.ACONST_NULL);
		default:
			throw new DiSLFatalException (
				"No default value for type: "+ type.getDescriptor ()
			);
		}
	}


	public static AbstractInsnNode getField (
		final String owner, final String name, final String desc
	) {
		return new FieldInsnNode (Opcodes.GETFIELD, owner, name, desc);
	}


	public static AbstractInsnNode getStatic (
		final String owner, final String name, final String desc
	) {
		return new FieldInsnNode (Opcodes.GETSTATIC, owner, name, desc);
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

	public static class ClonedCode {

		private InsnList instructions;
		private List<TryCatchBlockNode> tryCatchBlocks;

		public ClonedCode(InsnList instructions,
				List<TryCatchBlockNode> tryCatchBlocks) {
			super();
			this.instructions = instructions;
			this.tryCatchBlocks = tryCatchBlocks;
		}

		public InsnList getInstructions() {
			return instructions;
		}

		public List<TryCatchBlockNode> getTryCatchBlocks() {
			return tryCatchBlocks;
		}
	}

	public static ClonedCode cloneCode(InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks) {

		Map<LabelNode, LabelNode> tmpLblMap =
				AsmHelper.createLabelMap(instructions);

		InsnList clonedInstructions =
				AsmHelper.cloneInsnList(instructions, tmpLblMap);
		List<TryCatchBlockNode> clonedTryCatchBlocks =
				AsmHelper.cloneTryCatchBlocks(tryCatchBlocks, tmpLblMap);

		return new ClonedCode(clonedInstructions, clonedTryCatchBlocks);
	}

	// makes a clone of an instruction list
	public static InsnList cloneInsnList(InsnList src) {

		Map<LabelNode, LabelNode> map = createLabelMap(src);
		return cloneInsnList(src, map);
	}

	private static Map<LabelNode, LabelNode> createLabelMap(InsnList src) {

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

	private static InsnList cloneInsnList(InsnList src,
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

	private static List<TryCatchBlockNode> cloneTryCatchBlocks(
			List<TryCatchBlockNode> src, Map<LabelNode, LabelNode> map) {

		List<TryCatchBlockNode> dst = new LinkedList<TryCatchBlockNode>();

		for (TryCatchBlockNode tcb : src) {

			dst.add(new TryCatchBlockNode(map.get(tcb.start), map.get(tcb.end),
					map.get(tcb.handler), tcb.type));
		}

		return dst;
	}

	public static AbstractInsnNode skipVirtualInsns(AbstractInsnNode instr,
			boolean isForward) {

		while (instr != null && isVirtualInstr(instr)) {
			instr = isForward ? instr.getNext() : instr.getPrevious();
		}

		return instr;
	}

	/**
	 * Returns the first non-virtual instruction preceding a given instruction.
	 *
	 * @param startInsn the starting instruction
	 *
	 * @return
	 *     The first non-virtual instruction preceding the given instruction,
	 *     or {@code null} if there is no such instruction.
	 */
	public static AbstractInsnNode prevNonVirtualInsn (final AbstractInsnNode startInsn) {
		AbstractInsnNode insn = startInsn;
		while (insn != null) {
			insn = insn.getPrevious ();
			if (! isVirtualInstr (insn)) {
				return insn;
			}
		}

		// not found
		return null;
	}

	/**
	 * Returns the first non-virtual instruction following a given instruction.
	 *
	 * @param startInsn the starting instruction
	 *
	 * @return
	 *     The first non-virtual instruction following the given instruction,
	 *     or {@code null} if there is no such instruction.
	 */
	public static AbstractInsnNode nextNonVirtualInsn (final AbstractInsnNode start) {
		AbstractInsnNode insn = start;
		while (insn != null) {
			insn = insn.getNext ();
			if (! isVirtualInstr (insn)) {
				return insn;
			}
		}

		// not found
		return null;
	}


	public static boolean isReferenceType(Type type) {
		return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
	}

	public static boolean isVirtualInstr(AbstractInsnNode insn) {
		return insn.getOpcode() == -1;
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

	// helper method for boxValueOnStack
	private static MethodInsnNode constructValueOf (
		final Class <?> boxClass, final Class <?> primitiveClass
	) {
		final Type boxType = Type.getType (boxClass);
		final Type primitiveType = Type.getType (primitiveClass);

		final String descriptor = String.format (
			"(%s)%s", primitiveType.getDescriptor (), boxType.getDescriptor ()
		);

		return new MethodInsnNode (
			Opcodes.INVOKESTATIC,
			boxType.getInternalName() /* method owner */,
			"valueOf" /* method name */,
			descriptor /* method descriptor */
		);
	}


	/**
	 * Returns instruction that will call the method to box the instruction
	 * residing on the stack
	 *
	 * @param valueType type to be boxed
	 */
	public static AbstractInsnNode boxValueOnStack (final Type valueType) {
		switch (valueType.getSort ()) {
		case Type.BOOLEAN:
			return constructValueOf (Boolean.class, boolean.class);
		case Type.BYTE:
			return constructValueOf (Byte.class, byte.class);
		case Type.CHAR:
			return constructValueOf (Character.class, char.class);
		case Type.DOUBLE:
			return constructValueOf (Double.class, double.class);
		case Type.FLOAT:
			return constructValueOf (Float.class, float.class);
		case Type.INT:
			return constructValueOf (Integer.class, int.class);
		case Type.LONG:
			return constructValueOf (Long.class, long.class);
		case Type.SHORT:
			return constructValueOf (Short.class, short.class);

		default:
			throw new DiSLFatalException (
				"Impossible to box type: "+ valueType.getDescriptor ()
			);
		}
	}


	// Get the first valid mark of a method.
	// For a constructor, the return value will be the instruction after
	// the object initialization.
	public static AbstractInsnNode findFirstValidMark(MethodNode method) {

		AbstractInsnNode first = method.instructions.getFirst();

		// This is not a constructor. Just return the first instruction
		if (!method.name.equals(Constants.CONSTRUCTOR_NAME)) {
			return first;
		}

		// AdviceAdapter will help us with identifying the proper place where
		// the constructor to super is called

		// just need an object that will hold a value
		// - we need access to the changeable boolean via reference
		class DataHolder {
			boolean trigger = false;
		}
		final DataHolder dh = new DataHolder();

		MethodVisitor emptyVisitor = new MethodVisitor (Opcodes.ASM4) {};
		AdviceAdapter adapter = new AdviceAdapter (
			Opcodes.ASM4, emptyVisitor,
			method.access, method.name, method.desc
		) {
			public void onMethodEnter () {
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

}
