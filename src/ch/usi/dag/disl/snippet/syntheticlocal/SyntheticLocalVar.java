package ch.usi.dag.disl.snippet.syntheticlocal;

import org.objectweb.asm.tree.InsnList;

public class SyntheticLocalVar {

	public final static String NAME_DELIM = ".";
	
	private String className;
	private String fieldName;
	private InsnList initASMCode;
	
	public SyntheticLocalVar(String className, String fieldName) {
		super();
		this.className = className;
		this.fieldName = fieldName;
	}
	
	public InsnList getInitASMCode() {
		return initASMCode;
	}

	public void setInitASMCode(InsnList initASMCode) {
		this.initASMCode = initASMCode;
	}
	
	public String getID() {
		return className + NAME_DELIM + fieldName;
	}
}
