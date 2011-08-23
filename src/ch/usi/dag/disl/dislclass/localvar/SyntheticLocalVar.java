package ch.usi.dag.disl.dislclass.localvar;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;

public class SyntheticLocalVar extends AbstractLocalVar {

	private Type type;
	private SyntheticLocal.Initialize initialize;
	private InsnList initASMCode;
	
	public SyntheticLocalVar(String className, String fieldName, Type type, 
			SyntheticLocal.Initialize initialize) {
		
		super(className, fieldName);
		this.type = type;
		this.initialize = initialize;
	}
	
	public Type getType() {
		return type;
	}
	
	public SyntheticLocal.Initialize getInitialize() {
		return initialize;
	}
	
	public InsnList getInitASMCode() {
		return initASMCode;
	}

	public void setInitASMCode(InsnList initASMCode) {
		this.initASMCode = initASMCode;
	}
}
