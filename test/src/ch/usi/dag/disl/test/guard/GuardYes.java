package ch.usi.dag.disl.test.guard;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Shadow;

public class GuardYes implements SnippetGuard, ProcessorMethodGuard {

	@Override
	public boolean isApplicable(Shadow shadow,
			ProcInvocation processorInvocation,
			ProcMethodInstance processorMethodInstance, Type exactType) {

		return true;
	}

	@Override
	public boolean isApplicable(Shadow shadow) {

		return true;
	}

}
