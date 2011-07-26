package ch.usi.dag.disl.snippet.syntheticlocal;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.annotation.SyntheticLocal;

public class SyntheticLocalVar {

	public final static String NAME_DELIM = ".";
	
	private String className;
	private String fieldName;
	private InsnList initASMCode;
	private SyntheticLocal.Initialize initialize;
	
	public SyntheticLocalVar(String className, String fieldName, 
			SyntheticLocal.Initialize initialize) {
		
		super();
		this.className = className;
		this.fieldName = fieldName;
		this.initialize = initialize;
	}
	
	public InsnList getInitASMCode() {
		return initASMCode;
	}

	public void setInitASMCode(InsnList initASMCode) {
		this.initASMCode = initASMCode;
	}

	public SyntheticLocal.Initialize getInitialize() {
		return initialize;
	}
	
	public String getID() {
		return className + NAME_DELIM + fieldName;
	}
}
