package ch.usi.dag.disl.dislclass.processor;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class ProcessorMethod {

	private ProcessorArgType type;
	private UnprocessedCode unprocessedCode;
	private Code code;
	
	public ProcessorMethod(ProcessorArgType type, UnprocessedCode unprocessedCode) {
		super();
		this.type = type;
		this.unprocessedCode = unprocessedCode;
	}
	
	public ProcessorArgType getType() {
		return type;
	}

	public Code getCode() {
		return code;
	}

	public void init(LocalVars allLVs) throws StaticAnalysisException,
			ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;
	}
}
