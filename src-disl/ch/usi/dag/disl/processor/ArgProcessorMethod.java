package ch.usi.dag.disl.processor;

import java.lang.reflect.Method;
import java.util.EnumSet;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;

public class ArgProcessorMethod {

	private String originClassName;
	private String originMethodName;
	
	private EnumSet<ArgProcessorKind> types;
	private Method guard;
	private ProcUnprocessedCode unprocessedCode;
	private ProcCode code;

	public ArgProcessorMethod(String originClassName, String originMethodName,
			EnumSet<ArgProcessorKind> types, Method guard,
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

	public EnumSet<ArgProcessorKind> getTypes() {
		return types;
	}

	public Method getGuard() {
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
