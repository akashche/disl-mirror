package ch.usi.dag.disl.snippet;

import ch.usi.dag.disl.processor.Proc;
import ch.usi.dag.disl.processorcontext.ProcessorMode;

public class ProcInvocation {

	private Proc processor;
	private ProcessorMode procApplyType;
	
	public ProcInvocation(Proc processor, ProcessorMode procApplyType) {
		super();
		this.processor = processor;
		this.procApplyType = procApplyType;
	}

	public Proc getProcessor() {
		return processor;
	}

	public ProcessorMode getProcApplyType() {
		return procApplyType;
	}
}
