package ch.usi.dag.disl.dislclass.snippet;

import ch.usi.dag.disl.dislclass.processor.Processor;
import ch.usi.dag.disl.processor.ProcApplyType;

public class ProcessorInvocation {

	private Processor processor;
	private ProcApplyType procApplyType;
	
	public ProcessorInvocation(Processor processor, ProcApplyType procApplyType) {
		super();
		this.processor = processor;
		this.procApplyType = procApplyType;
	}

	public Processor getProcessor() {
		return processor;
	}

	public ProcApplyType getProcApplyType() {
		return procApplyType;
	}
}
