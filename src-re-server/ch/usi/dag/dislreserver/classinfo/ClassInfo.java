package ch.usi.dag.dislreserver.classinfo;

import ch.usi.dag.dislreserver.netreference.NetReference;

public class ClassInfo extends ExtractedClassInfo {

	private int classId;
	private String classSignature;
	private String classGenericStr;
	private NetReference classLoaderNR;
	private ClassInfo superClassInfo;
	
	public ClassInfo(int classId, String classSignature,
			String classGenericStr, NetReference classLoaderNR,
			ClassInfo superClassInfo, ExtractedClassInfo eci) {
		super(eci);
		this.classId = classId;
		this.classSignature = classSignature;
		this.classGenericStr = classGenericStr;
		this.classLoaderNR = classLoaderNR;
		this.superClassInfo = superClassInfo;
	}

	public int getClassId() {
		return classId;
	}
	
	public String getClassSignature() {
		return classSignature;
	}

	public String getClassGenericStr() {
		return classGenericStr;
	}

	public NetReference getClassLoaderNR() {
		return classLoaderNR;
	}
	
	public ClassInfo getSuperClassInfo() {
		return superClassInfo;
	}
}
