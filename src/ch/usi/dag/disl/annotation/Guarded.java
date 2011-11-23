package ch.usi.dag.disl.annotation;

public @interface Guarded {

	// NOTE if you want to change names, you need to change 
	// ProcessorParser.ProcMethodAnnotationData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	Class<? extends Object> guard();
}
