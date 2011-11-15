package ch.usi.dag.disl.annotation;

import ch.usi.dag.disl.guard.ProcessorGuard;

public @interface ArgsProcessor {

	// NOTE if you want to change names, you need to change 
	// ProcessorParser.ProcessorAnnotationData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	Class<? extends ProcessorGuard> guard() default ProcessorGuard.class; // cannot be null :(
}
