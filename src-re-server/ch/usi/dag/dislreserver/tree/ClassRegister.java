package ch.usi.dag.dislreserver.tree;

import java.util.HashMap;

public class ClassRegister {

	private static HashMap<String, ClassRef> classes = 
		new HashMap<String, ClassRef>();

	public static void register(byte[] clazz) {
		ClassRef info = new ClassRef(clazz);
		classes.put(info.getName().replace('/', '.'), info);
	}

	public static ClassRef forName(String clazz) {
		return classes.get(clazz);
	}

}
