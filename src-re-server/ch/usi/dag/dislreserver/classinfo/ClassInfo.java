package ch.usi.dag.dislreserver.classinfo;

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
	private byte[] classCode;

	public ClassInfo(int classId, String classSignature,
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
		this.classCode = classCode;
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

	public byte[] getClassCode() {
		return classCode;
	}
}
