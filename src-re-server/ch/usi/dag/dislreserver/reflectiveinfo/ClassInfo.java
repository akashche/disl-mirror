package ch.usi.dag.dislreserver.reflectiveinfo;

import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfo {

	private int classId;
	private String classSignature;
	private String classGenericStr;
	private boolean classIsArray;
	private int arrayDimensions;
	private ClassInfo arrayComponentType;
	private NetReference classLoaderNR;
	private ClassInfo superClassInfo;

	private ClassInfo(int classId, String classSignature,
			String classGenericStr, boolean classIsArray, int arrayDimensions,
			ClassInfo arrayComponentType, NetReference classLoaderNR,
			ClassInfo superClassInfo, byte[] classCode) {
		super();
		this.classId = classId;
		this.classSignature = classSignature;
		this.classGenericStr = classGenericStr;
		this.classIsArray = classIsArray;
		this.arrayDimensions = arrayDimensions;
		this.arrayComponentType = arrayComponentType;
		this.classLoaderNR = classLoaderNR;
		this.superClassInfo = superClassInfo;

		initializeClassInfo(classCode);
	}

	public int getId() {
		return classId;
	}
	
	public String getSignature() {
		return classSignature;
	}

	public String getGenericStr() {
		return classGenericStr;
	}

	public boolean isArray() {
		return classIsArray;
	}

	public int getArrayDimensions() {
		return arrayDimensions;
	}

	public ClassInfo getComponentType() {
		return arrayComponentType;
	}

	public NetReference getClassLoaderNR() {
		return classLoaderNR;
	}
	
	public ClassInfo getSuperClassInfo() {
		return superClassInfo;
	}


    private void initializeClassInfo(byte[] classCode) {
        //TODO: add the code to:
        //*) parse classCode using ASM
        //*) initialize all fields required for the following methods
    }

    //All these methods should return the value of fields initialized by generateClassInfo(byte[])
    //NOTE: returned arrays should be copies (obtained using Arrays.copyOf(...)) 
	public boolean isInstance(NetReference nr){ return false; }
	public boolean isAssignableFrom(ClassInfo ci){ return false; }
	public boolean isInterface(){ return false; }
	public boolean isPrimitive(){ return false; }
	public boolean isAnnotation(){ return false; }
	public boolean isSynthetic(){ return false; }
	public boolean isEnum(){ return false; }

	public String getName(){ return null; }
	public String getCanonicalName(){ return null; }

	public String[] getInterfaces(){ return null; }
	public String getPackage(){ return null; }
	public ClassInfo getSuperclass(){ return null; }

	public FieldInfo[] getFields(){ return null; }
	public FieldInfo getField(String fieldName) throws NoSuchFieldException{ return null; }
	public MethodInfo[] getMethods(){ return null; }
	public MethodInfo getMethod(String methodName, ClassInfo[] argumentCIs) throws NoSuchMethodException{ return null; }
	public MethodInfo getMethod(String methodName, String[] argumentNames) throws NoSuchMethodException{ return null; }

	public String[] getDeclaredClasses(){ return null; }
	public FieldInfo[] getDeclaredFields(){ return null; }
	public FieldInfo getDeclaredField(String fieldName) throws NoSuchFieldException{ return null; }
	public MethodInfo[] getDeclaredMethods(){ return null; }
	public MethodInfo getDeclaredMethod(String methodName, ClassInfo[] argumentCIs) throws NoSuchMethodException{ return null; }
	public MethodInfo getDeclaredMethod(String methodName, String[] argumentNames) throws NoSuchMethodException{ return null; }
}
