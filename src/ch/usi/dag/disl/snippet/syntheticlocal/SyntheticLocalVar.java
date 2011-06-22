package ch.usi.dag.disl.snippet.syntheticlocal;

import org.objectweb.asm.tree.InsnList;

public class SyntheticLocalVar {

	final static String NAME_DELIM = ".";
	
	private String className;
	private String methodName;
	private InsnList initASMCode;
	
	public SyntheticLocalVar(String className, String methodName) {
		super();
		this.className = className;
		this.methodName = methodName;
	}
	
	public InsnList getInitASMCode() {
		return initASMCode;
	}

	public void setInitASMCode(InsnList initASMCode) {
		this.initASMCode = initASMCode;
	}
	
	public String getID() {
		return className + NAME_DELIM + methodName;
	}
}
