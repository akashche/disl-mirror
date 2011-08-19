package ch.usi.dag.disl.processor.generator;

import java.util.List;

import ch.usi.dag.disl.processor.ProcApplyType;

public class ProcessorInstance {

	private ProcApplyType procApplyType;
	private List<ProcessorMethodInstance> methods;

	public ProcessorInstance(ProcApplyType procApplyType,
			List<ProcessorMethodInstance> methods) {
		super();
		this.methods = methods;
	}

	public ProcApplyType getProcApplyType() {
		return procApplyType;
	}
	
	public List<ProcessorMethodInstance> getMethods() {
		return methods;
	}
}
