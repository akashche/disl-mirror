package ch.usi.dag.disl.guard;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Shadow;

public interface ProcessorMethodGuard {

	public boolean isApplicable(Shadow shadow,
			ProcInvocation processorInvocation,
			ProcMethodInstance processorMethodInstance,
			Type exactType);
}
