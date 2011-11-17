package ch.usi.dag.disl.processor.generator;

import ch.usi.dag.disl.processor.generator.struct.ProcArgType;
import ch.usi.dag.disl.processor.generator.struct.ProcCode;


public class ProcMethodInstance {

	private int argPos;
	private int argsCount;
	private ProcArgType argType;
	private String argTypeName;
	private ProcCode code;
	
	public ProcMethodInstance(int argPos, int argsCount, ProcArgType argType,
			ProcCode code) {
		super();
		this.argPos = argPos;
		this.argsCount = argsCount;
		this.argType = argType;
		this.code = code;
	}

	public int getArgPos() {
		return argPos;
	}

	public int getArgsCount() {
		return argsCount;
	}

	public ProcArgType getArgType() {
		return argType;
	}

	public String getArgTypeName() {
		return argTypeName;
	}

	public void setArgTypeName(String argTypeName) {
		this.argTypeName = argTypeName;
	}
	
	// Note: Code is NOT cloned for each instance of ProcMethodInstance.
	// If the weaver does not rely on this, we can reuse processor instances
	// which can save us some computation
	public ProcCode getCode() {
		return code;
	}
}
