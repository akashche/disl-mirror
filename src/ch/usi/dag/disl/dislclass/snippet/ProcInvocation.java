package ch.usi.dag.disl.dislclass.snippet;

import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.processor.ProcessorApplyType;

public class ProcInvocation {

	private Proc processor;
	private ProcessorApplyType procApplyType;
	
	public ProcInvocation(Proc processor, ProcessorApplyType procApplyType) {
		super();
		this.processor = processor;
		this.procApplyType = procApplyType;
	}

	public Proc getProcessor() {
		return processor;
	}

	public ProcessorApplyType getProcApplyType() {
		return procApplyType;
	}
}
