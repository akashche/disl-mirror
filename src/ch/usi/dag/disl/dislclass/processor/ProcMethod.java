package ch.usi.dag.disl.dislclass.processor;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;

public class ProcMethod {

	private ProcArgType type;
	private ProcessorMethodGuard guard;
	private UnprocessedCode unprocessedCode;
	private Code code;

	public ProcMethod(ProcArgType type, ProcessorMethodGuard guard,
			UnprocessedCode unprocessedCode) {
		super();
		this.type = type;
		this.guard = guard;
		this.unprocessedCode = unprocessedCode;
	}

	public ProcArgType getType() {
		return type;
	}

	public ProcessorMethodGuard getGuard() {
		return guard;
	}

	public Code getCode() {
		return code;
	}

	public void init(LocalVars allLVs) throws StaticInfoException,
			ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;
	}
}
