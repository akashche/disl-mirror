package ch.usi.dag.disl.processor.generator;

import java.util.List;

import ch.usi.dag.disl.processor.ProcessorMode;

public class ProcInstance {

	private ProcessorMode procApplyType;
	private List<ProcMethodInstance> methods;

	public ProcInstance(ProcessorMode procApplyType,
			List<ProcMethodInstance> methods) {
		super();
		this.procApplyType = procApplyType;
		this.methods = methods;
	}

	public ProcessorMode getProcApplyType() {
		return procApplyType;
	}
	
	public List<ProcMethodInstance> getMethods() {
		return methods;
	}
}
