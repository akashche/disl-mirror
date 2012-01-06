package ch.usi.dag.disl.weaver.pe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ch.usi.dag.disl.util.Constants;

public class InvocationInterpreter {

	public final static ArrayList<String> CONST = new ArrayList<String>();

	static {
		CONST.add("java/lang/Boolean");
		CONST.add("java/lang/Byte");
		CONST.add("java/lang/Character");
		CONST.add("java/lang/Double");
		CONST.add("java/lang/Float");
		CONST.add("java/lang/Integer");
		CONST.add("java/lang/Long");
		CONST.add("java/lang/Short");
		CONST.add("java/lang/String");
	}

	private static InvocationInterpreter instance = null;

	private HashSet<String> registeredMethods;

	private InvocationInterpreter() {
		registeredMethods = new HashSet<String>();
	}

	private Class<?> getClassFromType(Type type) throws ClassNotFoundException {

		switch (type.getSort()) {

		case Type.BOOLEAN:
			return boolean.class;
		case Type.BYTE:
			return byte.class;
		case Type.CHAR:
			return char.class;
		case Type.DOUBLE:
			return double.class;
		case Type.FLOAT:
			return float.class;
		case Type.INT:
			return int.class;
		case Type.LONG:
			return long.class;
		case Type.SHORT:
			return short.class;
		case Type.OBJECT:
			return Class.forName(type.getClassName());
		default:
			return null;
		}
	}

	private boolean isConstType(Type type) {

		switch (type.getSort()) {

		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.DOUBLE:
		case Type.FLOAT:
		case Type.INT:
		case Type.LONG:
		case Type.SHORT:
			return true;

		case Type.OBJECT:
			return CONST.contains(type.getInternalName());

		default:
			return false;
		}
	}

	private Class<?>[] getClasses(String desc) throws ClassNotFoundException {
		ArrayList<Class<?>> list = new ArrayList<Class<?>>();

		Type[] types = Type.getArgumentTypes(desc);

		for (Type type : types) {

			Class<?> clazz = getClassFromType(type);

			if (clazz == null) {
				return null;
			}

			list.add(clazz);
		}

		return list.toArray(new Class<?>[list.size()]);
	}

	private Object getObj(MethodInsnNode instr,
			List<? extends ConstValue> values) {

		if (instr.getOpcode() == Opcodes.INVOKESTATIC) {
			return null;
		} else {

			Object obj = values.get(0).cst;

			if (obj instanceof Reference) {

				Reference ref = (Reference) obj;

				if (ref.isValid()) {
					return ref.getObj();
				} else {
					return null;
				}
			}

			return obj;
		}
	}

	private Object[] getArgs(MethodInsnNode instr,
			List<? extends ConstValue> values, Class<?>[] parameters) {

		if (instr.getOpcode() == Opcodes.INVOKEVIRTUAL) {

			Object[] args = new Object[values.size() - 1];

			for (int i = 0; i < args.length; i++) {
				args[i] = castFromInteger(values.get(i + 1).cst, parameters[i]);
			}

			return args;
		} else if (instr.getOpcode() == Opcodes.INVOKESTATIC) {

			Object[] args = new Object[values.size()];

			for (int i = 0; i < args.length; i++) {
				args[i] = castFromInteger(values.get(i).cst, parameters[i]);
			}

			return args;
		}

		return null;
	}

	private Object castFromInteger(Object obj, Class<?> clazz) {

		if (!(obj instanceof Integer)) {
			return obj;
		}

		int i = (Integer) obj;

		if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
			return i == 1;
		}

		if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
			return (byte) i;
		}

		if (clazz.equals(Character.class) || clazz.equals(char.class)) {
			return (char) i;
		}

		if (clazz.equals(Short.class) || clazz.equals(short.class)) {
			return (short) i;
		}

		return obj;
	}

	private Object castToInteger(Object obj, Class<?> clazz) {

		if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
			return ((Boolean) obj) ? 1 : 0;
		}

		if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
			return (int) (byte) (Byte) obj;
		}

		if (clazz.equals(Character.class) || clazz.equals(char.class)) {
			return (int) (char) (Character) obj;
		}

		if (clazz.equals(Short.class) || clazz.equals(short.class)) {
			return (int) (short) (Short) obj;
		}

		return obj;
	}

	public Object execute(MethodInsnNode instr,
			List<? extends ConstValue> values) {

		if (instr.getOpcode() == Opcodes.INVOKEDYNAMIC) {
			return null;
		}

		// TODO remove hack of StringBuilder
		if (instr.getOpcode() == Opcodes.INVOKESPECIAL) {
			// TRICK: Special Case for Constructing String Builder
			// NOTE that normally the constructor returns nothing,
			// but before the constructor, the reference is duplicated
			// on the stack.
			if (instr.owner.equals("java/lang/StringBuilder")
					&& instr.name.equals("<init>") && values.size() > 1) {

				Object obj = getObj(instr, values);
				Object cst = values.get(1).cst;
				
				if (obj == null){
					return null;
				}

				if (cst == null) {
					((Reference) obj).setValid(false);
					return null;
				}
				
				if ((cst instanceof String)) {
					((StringBuilder) obj).append(cst);
				}
			}

			return null;
		}

		if (!registeredMethods.contains(getMethodID(instr))) {
			return null;
		}

		for (ConstValue value : values) {
			if (value.cst == null) {
				return null;
			}
		}
		
		try {

			Class<?> clazz = Class.forName(instr.owner.replace('/', '.'));
			Class<?>[] parameters = getClasses(instr.desc);
			Class<?> retType = getClassFromType(Type.getReturnType(instr.desc));

			if (parameters == null || retType == null) {
				return null;
			}

			Object obj = getObj(instr, values);
			Object[] args = getArgs(instr, values, parameters);

			if (args == null) {
				return null;
			}

			Object retValue = clazz.getMethod(instr.name, parameters).invoke(
					obj, args);

			if (!isConstType(Type.getType(retValue.getClass()))) {
				return null;
			}

			return castToInteger(retValue, retType);
		} catch (Exception e) {
			return null;
		}
	}

	public void register(String full) {
		registeredMethods.add(full);
	}

	public void register(String owner, String name, String desc) {
		register(owner + Constants.CLASS_DELIM + name + desc);
	}

	public void register(String owner, Method method) {
		register(owner, method.getName(), Type.getMethodDescriptor(method));
	}

	public void register(Class<?> clazz) {

		String owner = Type.getInternalName(clazz);
		CONST.add(owner);

		for (Method method : clazz.getMethods()) {
			if (!method.getName().endsWith("init>")) {
				register(owner, method);
			}
		}
	}

	private String getMethodID(MethodInsnNode min) {
		return min.owner + Constants.CLASS_DELIM + min.name + min.desc;
	}

	public boolean isRegistered(AbstractInsnNode instr) {

		if (!(instr instanceof MethodInsnNode)) {
			return false;
		}

		MethodInsnNode min = (MethodInsnNode) instr;

		// TODO remove hack of StringBuilder
		if (min.getOpcode() == Opcodes.INVOKESPECIAL) {
			if (min.owner.equals("java/lang/StringBuilder")
					&& min.name.equals("<init>")) {
				return true;
			}
		}
		
		if (!(instr.getOpcode() == Opcodes.INVOKEVIRTUAL || instr.getOpcode() == Opcodes.INVOKESTATIC)) {
			return false;
		}

		return registeredMethods.contains(getMethodID((MethodInsnNode) instr));
	}

	public static InvocationInterpreter getInstance() {

		if (instance == null) {
			instance = new InvocationInterpreter();
			instance.register(Boolean.class);
			instance.register(Byte.class);
			instance.register(Character.class);
			instance.register(Double.class);
			instance.register(Float.class);
			instance.register(Integer.class);
			instance.register(Long.class);
			instance.register(Short.class);
			instance.register(String.class);
			instance.register(StringBuilder.class);
		}

		return instance;
	}
}
