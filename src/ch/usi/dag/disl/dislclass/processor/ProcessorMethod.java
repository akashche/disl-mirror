package ch.usi.dag.disl.dislclass.processor;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class ProcessorMethod {

	protected ProcessorType type;
	protected UnprocessedCode unprocessedCode;
	protected Code code;
	
	public ProcessorMethod(ProcessorType type, UnprocessedCode unprocessedCode) {
		super();
		this.type = type;
		this.unprocessedCode = unprocessedCode;
	}
	
	public ProcessorType getType() {
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
