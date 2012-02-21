package ch.usi.dag.dislreserver.classid;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class ClassIdResolver {

	// TODO re ! change object
	static Map<Long, Map<String, Object>> classLoaderMap = 
			new HashMap<Long, Map<String, Object>>();
	
	// TODO re ! change object
	static Map<Integer, Object> classIdMap =
			new HashMap<Integer, Object>();

	// TODO re ! complete holder
	
	public static void addNewClass(String className, long classLoderId, byte[] classCode) {
		
		// TODO re ! change object
		Map<String, Object> classNameMap = classLoaderMap.get(classLoderId);
		
		if(classNameMap == null) {
			// TODO re ! change object
			classNameMap = new HashMap<String, Object>();
		}
		
		// TODO re ! change object
		classNameMap.put(className, new Object());
	}
	
	public static void createHierarchy(String className, long classLoderId, int classId, int superId) {
		
		// TODO re ! change object
		Map<String, Object> classNameMap = classLoaderMap.get(classLoderId);

		if(classNameMap == null) {
			throw new DiSLREServerFatalException("Class loader not known");
		}

		// TODO re ! change object
		Object obj = classNameMap.get(className);
		
		if(obj == null) {
			throw new DiSLREServerFatalException("Class not known");
		}
		
		// TODO re ! register hierarchy
	}
	
	// TODO re ! change object
	public static Object getClass(int classId) {
		return classIdMap.get(classId);
	}
}
