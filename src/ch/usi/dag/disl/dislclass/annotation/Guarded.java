package ch.usi.dag.disl.dislclass.annotation;

import ch.usi.dag.disl.guard.ProcessorMethodGuard;

public @interface Guarded {

	// NOTE if you want to change names, you need to change ProcessorParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser

	Class<? extends ProcessorMethodGuard> guard() default ProcessorMethodGuard.class; // cannot be null :(
}
