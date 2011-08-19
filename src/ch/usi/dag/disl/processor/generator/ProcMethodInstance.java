package ch.usi.dag.disl.processor.generator;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.processor.ProcArgType;


public class ProcMethodInstance {

	private int argPos;
	private int argsCount;
	private ProcArgType argType;
	private Code code;
	
	public ProcMethodInstance(int argPos, int argsCount,
			ProcArgType argType, Code code) {
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

	// Note: Code is NOT cloned for each instance of ProcMethodInstance.
	// If the weaver does not rely on this, we can reuse processor instances
	// which can save us some computation
	public Code getCode() {
		return code;
	}
}
