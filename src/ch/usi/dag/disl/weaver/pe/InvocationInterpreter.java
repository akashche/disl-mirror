package ch.usi.dag.disl.weaver.pe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class InvocationInterpreter {

	private static InvocationInterpreter instance = null;

	private HashSet<String> registeredClasses;

	private InvocationInterpreter() {
		registeredClasses = new HashSet<String>();
	}

	private void register(Class<?> clazz) {
		registeredClasses.add(Type.getInternalName(clazz));
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

		if (instr.getOpcode() == Opcodes.INVOKEVIRTUAL) {
			return values.get(0).cst;
		} else {
			return null;
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

		if (instr.getOpcode() == Opcodes.INVOKESPECIAL
				|| instr.getOpcode() == Opcodes.INVOKEDYNAMIC) {
			return null;
		}

		if (!registeredClasses.contains(instr.owner)) {
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

			if (!registeredClasses.contains(Type.getInternalName(retValue
					.getClass()))) {
				return null;
			}

			return castToInteger(retValue, retType);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean isRegistered(AbstractInsnNode instr) {

		if (!(instr.getOpcode() == Opcodes.INVOKEVIRTUAL || instr.getOpcode() == Opcodes.INVOKESTATIC)) {
			return false;
		}

		return registeredClasses.contains(((MethodInsnNode) instr).owner);
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
		}

		return instance;
	}
}
