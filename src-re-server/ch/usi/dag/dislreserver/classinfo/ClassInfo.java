package ch.usi.dag.dislreserver.classinfo;

import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfo extends ExtractedClassInfo {

	private int classId;
	private String classSignature;
	private String classGenericStr;
	private boolean classIsArray;
	private int arrayDimensions;
	private NetReference classLoaderNR;
	private ClassInfo superClassInfo;
	
	public ClassInfo(int classId, String classSignature,
			String classGenericStr, boolean classIsArray, int arrayDimensions,
			NetReference classLoaderNR, ClassInfo superClassInfo,
			ExtractedClassInfo eci) {
		super(eci);
		this.classId = classId;
		this.classSignature = classSignature;
		this.classGenericStr = classGenericStr;
		this.classIsArray = classIsArray;
		this.arrayDimensions = arrayDimensions;
		this.classLoaderNR = classLoaderNR;
		this.superClassInfo = superClassInfo;
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

	public NetReference getClassLoaderNR() {
		return classLoaderNR;
	}
	
	public ClassInfo getSuperClassInfo() {
		return superClassInfo;
	}
}
