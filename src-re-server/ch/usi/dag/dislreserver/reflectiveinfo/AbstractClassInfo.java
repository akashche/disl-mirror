package ch.usi.dag.dislreserver.reflectiveinfo;

import ch.usi.dag.dislreserver.netreference.NetReference;

public abstract class AbstractClassInfo implements ClassInfo {

	private int classId;
	private String classSignature;
	private String classGenericStr;
	private NetReference classLoaderNR;
	private ClassInfo superClassInfo;

	public AbstractClassInfo(int classId, String classSignature,
			String classGenericStr, NetReference classLoaderNR,
			ClassInfo superClassInfo) {
		super();
		this.classId = classId;
		this.classSignature = classSignature;
		this.classGenericStr = classGenericStr;
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

	public NetReference getClassLoaderNR() {
		return classLoaderNR;
	}

	public ClassInfo getSuperclass() {
		return superClassInfo;
	}
}
