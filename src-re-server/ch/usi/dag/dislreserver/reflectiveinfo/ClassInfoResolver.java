package ch.usi.dag.dislreserver.reflectiveinfo;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfoResolver {

	static Map<Long, Map<String, byte[]>> classLoaderMap = 
			new HashMap<Long, Map<String, byte[]>>();
	
	// resolve class info by id
	static Map<Integer, ClassInfo> classIdMap =
			new HashMap<Integer, ClassInfo>();
	
	public static void addNewClass(String className, NetReference classLoaderNR,
			byte[] classCode) {
		
		Map<String, byte[]> classNameMap = 
				classLoaderMap.get(classLoaderNR.getObjectId());
		
		if(classNameMap == null) {
			classNameMap = new HashMap<String, byte[]>();
			classLoaderMap.put(classLoaderNR.getObjectId(), classNameMap);
		}
		
		classNameMap.put(className, classCode);
	}
	
	public static ClassInfo createHierarchy(String classSignature,
			String classGenericStr, NetReference classLoaderNR, int classId,
			int superClassId) {

		// create asm type to get class name
		Type classASMType = Type.getType(classSignature);

		// resolve super class
		ClassInfo superClassInfo = classIdMap.get(superClassId);

		// array handling
		if(classASMType.getSort() == Type.ARRAY) {
			
			// TODO ! re - send arrayComponentId from client
			
			ClassInfo classInfo = new ArrayClassInfo(classId, classSignature,
					classGenericStr, classLoaderNR, superClassInfo,
					classASMType.getDimensions(), null);
			
			classIdMap.put(classId, classInfo);
			return classInfo;
		}
		
		// basic type handling
		if (classASMType.getSort() != Type.OBJECT) {
			
			ClassInfo classInfo = new PrimitiveClassInfo(classId,
					classSignature, classGenericStr, classLoaderNR,
					superClassInfo);
			
			classIdMap.put(classId, classInfo);
			return classInfo;
		}
		
		// object handling
		
		Map<String, byte[]> classNameMap = 
				classLoaderMap.get(classLoaderNR.getObjectId());

		// NOTE: It is not expected but this pointer can be NULL if this
		// class loader loaded some class in pre-init jvm phase and nothing
		// after that
		// the best is to resolve the problem as in the next if
		if(classNameMap == null) {
			throw new DiSLREServerFatalException("Class loader not known");
		}

		byte[] classCode = 
				classNameMap.get(classASMType.getInternalName());
		
		if(classCode == null) {

			// try to lookup the code from the pre-init jvm phase
			// the code is send with class loader 0 flag
			classNameMap = classLoaderMap.get(new Long(0));
			classCode = classNameMap.get(classASMType.getInternalName());
			
			// nothing found
			if(classCode == null) {
				throw new DiSLREServerFatalException(
						"Class not known: " + classASMType.getInternalName());
			}
		}
		
		ClassInfo classInfo = new CommonClassInfo(classId, classSignature,
				classGenericStr, classLoaderNR, superClassInfo, classCode);
		
		classIdMap.put(classId, classInfo);
		return classInfo;
	}
	
	public static ClassInfo getClass(int classId) {
		return classIdMap.get(classId);
	}
}
