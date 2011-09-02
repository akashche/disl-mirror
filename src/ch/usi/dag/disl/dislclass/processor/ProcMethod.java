package ch.usi.dag.disl.dislclass.processor;

import java.util.EnumSet;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;

public class ProcMethod {

	private EnumSet<ProcArgType> types;
	private boolean insertTypeName;
	private ProcessorMethodGuard guard;
	private UnprocessedCode unprocessedCode;
	private Code code;

	public ProcMethod(EnumSet<ProcArgType> types, boolean insertTypeName,
			ProcessorMethodGuard guard, UnprocessedCode unprocessedCode) {
		super();
		this.types = types;
		this.insertTypeName = insertTypeName;
		this.guard = guard;
		this.unprocessedCode = unprocessedCode;
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

	public void init(LocalVars allLVs) throws StaticInfoException,
			ReflectionException {

		code = unprocessedCode.process(allLVs);
		unprocessedCode = null;
	}
}
