package ch.usi.dag.dislreserver.classinfo;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfoResolver {

	static Map<Long, Map<String, byte[]>> classLoaderMap = 
			new HashMap<Long, Map<String, byte[]>>();
	
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
			
			// TODO re ! create specific ClassInfo for array
			//  - we need info about the inner type
			
			ClassInfo classInfo = new ClassInfo(classId, classSignature,
					classGenericStr, true, classASMType.getDimensions(), null,
					classLoaderNR, superClassInfo, new byte[0]);
			
			classIdMap.put(classId, classInfo);
			return classInfo;
		}
		
		// basic type handling
		if (classASMType.getSort() != Type.OBJECT) {
			
			// TODO re ! ExtractedClassInfo for basic types
			ClassInfo classInfo = new ClassInfo(classId, classSignature,
					classGenericStr, false, 0, null, null, null, new byte[0]);
			
			classIdMap.put(classId, classInfo);
			return classInfo;
		}
		
		// object handling
		
		Map<String, byte[]> classNameMap = 
				classLoaderMap.get(classLoaderNR.getObjectId());

		// TODO re ! this can be null because of bad class loader tagging
		if(classNameMap == null) {
			throw new DiSLREServerFatalException("Class loader not known");
		}

		byte[] classCode = 
				classNameMap.get(classASMType.getInternalName());
		
		if(classCode == null) {
			
			// TODO re ! should not be needed when class loader tagging is fixed
			classNameMap = classLoaderMap.get(new Long(0)); // something will be there
			classCode = classNameMap.get(classASMType.getInternalName());
			if(classCode == null) {
				System.err.println("Class not known: " + classASMType.getInternalName());
				return null;
			}
			
			// TODO re ! replace with this
			//throw new DiSLREServerFatalException("Class not known");
		}
		
		ClassInfo classInfo = new ClassInfo(classId, classSignature,
				classGenericStr, false, 0, null, classLoaderNR, superClassInfo,
				classCode);
		
		classIdMap.put(classId, classInfo);
		return classInfo;
	}
	
	public static ClassInfo getClass(int classId) {
		return classIdMap.get(classId);
	}
}
