package ch.usi.dag.disl.processor.generator.struct;

import java.util.EnumSet;

import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.coderep.UnprocessedCode;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.localvar.LocalVars;

public class ProcMethod {

	private String originClassName;
	private String originMethodName;
	
	private EnumSet<ProcArgType> types;
	private boolean insertTypeName;
	private ProcessorMethodGuard guard;
	private UnprocessedCode unprocessedCode;
	private Code code;

	public ProcMethod(String originClassName, String originMethodName,
			EnumSet<ProcArgType> types, boolean insertTypeName,
			ProcessorMethodGuard guard, UnprocessedCode unprocessedCode) {
		super();
		this.originClassName = originClassName;
		this.originMethodName = originMethodName;
		this.types = types;
		this.insertTypeName = insertTypeName;
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

	public boolean insertTypeName() {
		return insertTypeName;
	}

	public ProcessorMethodGuard getGuard() {
		return guard;
	}

	public Code getCode() {
		return code;
	}

	public void init(LocalVars allLVs) throws StaticContextGenException,
			ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;
	}
}
