package ch.usi.dag.disl.snippet.processor;

import java.util.EnumSet;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.localvar.LocalVars;

public class ProcMethod {

	private String originClassName;
	private String originMethodName;
	
	private EnumSet<ProcArgType> types;
	private ProcessorMethodGuard guard;
	private ProcUnprocessedCode unprocessedCode;
	private ProcCode code;

	public ProcMethod(String originClassName, String originMethodName,
			EnumSet<ProcArgType> types, ProcessorMethodGuard guard,
			ProcUnprocessedCode unprocessedCode) {
		super();
		this.originClassName = originClassName;
		this.originMethodName = originMethodName;
		this.types = types;
		this.guard = guard;
		this.unprocessedCode = unprocessedCode;
	}

	public String getOriginClassName() {
		return originClassName;
	}

	public String getOriginMethodName() {
		return originMethodName;
	}

	public EnumSet<ProcArgType> getTypes() {
		return types;
	}

	public ProcessorMethodGuard getGuard() {
		return guard;
	}

	public ProcCode getCode() {
		return code;
	}

	public void init(LocalVars allLVs) throws StaticContextGenException,
			ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;
	}
}
