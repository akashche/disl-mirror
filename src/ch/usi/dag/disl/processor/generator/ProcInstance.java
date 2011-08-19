package ch.usi.dag.disl.processor.generator;

import java.util.List;

import ch.usi.dag.disl.processor.ProcessorApplyType;

public class ProcInstance {

	private ProcessorApplyType procApplyType;
	private List<ProcMethodInstance> methods;

	public ProcInstance(ProcessorApplyType procApplyType,
			List<ProcMethodInstance> methods) {
		super();
		this.methods = methods;
	}

	public ProcessorApplyType getProcApplyType() {
		return procApplyType;
	}
	
	public List<ProcMethodInstance> getMethods() {
		return methods;
	}
}
