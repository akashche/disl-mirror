package ch.usi.dag.disl.processor.generator;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.processor.ProcessorArgType;


public class ProcessorMethodInstance {

	private int argPos;
	private int argsCount;
	private ProcessorArgType argType;
	private Code code;
	
	// Note: clones code automatically
	public ProcessorMethodInstance(int argPos, int argsCount,
			ProcessorArgType argType, Code code) {
		super();
		this.argPos = argPos;
		this.argsCount = argsCount;
		this.argType = argType;
		// create clone automatically
		this.code = code.clone();
	}

	public int getArgPos() {
		return argPos;
	}

	public int getArgsCount() {
		return argsCount;
	}

	public ProcessorArgType getArgType() {
		return argType;
	}

	// Note: Code is cloned for each instance of ProcessorMethodInstance
	public Code getCode() {
		return code;
	}
}
