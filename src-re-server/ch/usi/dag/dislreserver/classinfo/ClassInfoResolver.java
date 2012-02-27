package ch.usi.dag.dislreserver.classinfo;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfoResolver {

	static Map<Long, Map<String, ExtractedClassInfo>> classLoaderMap = 
			new HashMap<Long, Map<String, ExtractedClassInfo>>();
	
	static Map<Integer, ClassInfo> classIdMap =
			new HashMap<Integer, ClassInfo>();

	public static void addNewClass(String className, NetReference classLoaderNR,
			byte[] classCode) {
		
		Map<String, ExtractedClassInfo> classNameMap = 
				classLoaderMap.get(classLoaderNR.getObjectId());
		
		if(classNameMap == null) {
			classNameMap = new HashMap<String, ExtractedClassInfo>();
			classLoaderMap.put(classLoaderNR.getObjectId(), classNameMap);
		}
		
		classNameMap.put(className, new ExtractedClassInfo(classCode));
	}
	
	public static void createHierarchy(String classSignature,
			String classGenericStr, NetReference classLoaderNR, int classId,
			int superClassId) {

		// create asm type to get class name
		Type classASMType = Type.getType(classSignature);
		
		boolean classIsArray = false;
		int arrayDimensions = 0;
		
		if(classASMType.getSort() == Type.ARRAY) {
			
			arrayDimensions = classASMType.getDimensions();
			classASMType = classASMType.getElementType();
			classIsArray = true;
		}
		
		// basic type handling
		if (classASMType.getSort() != Type.OBJECT) {
			
			// TODO re ! ExtractedClassInfo for basic types
			classIdMap.put(classId, new ClassInfo(classId, classSignature,
					classGenericStr, classIsArray, arrayDimensions,
					classLoaderNR, null, new ExtractedClassInfo(new byte[0])));
			return;
		}
		
		// object handling
		
		Map<String, ExtractedClassInfo> classNameMap = 
				classLoaderMap.get(classLoaderNR.getObjectId());

		if(classNameMap == null) {
			throw new DiSLREServerFatalException("Class loader not known");
		}

		ExtractedClassInfo eci = 
				classNameMap.get(classASMType.getInternalName());
		
		if(eci == null) {
			
			// TODO re ! should not be needed when class loader tagging is fixed
			classNameMap = classLoaderMap.get(new Long(0)); // something will be there
			eci = classNameMap.get(classASMType.getInternalName());
			if(eci == null) {
				System.err.println("Class not known: " + classASMType.getInternalName());
				return;
			}
			
			// TODO re ! replace with this
			//throw new DiSLREServerFatalException("Class not known");
		}
		
		// resolve super class
		ClassInfo superClassInfo = classIdMap.get(superClassId);
		
		classIdMap.put(classId, new ClassInfo(classId, classSignature,
				classGenericStr, classIsArray, arrayDimensions, classLoaderNR,
				superClassInfo, eci));
	}
	
	public static ClassInfo getClass(int classId) {
		return classIdMap.get(classId);
	}
}
