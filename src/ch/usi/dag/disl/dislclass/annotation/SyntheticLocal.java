package ch.usi.dag.disl.dislclass.annotation;

// NOTE: Initialization can be done only within field definition
// - java static { } construct for initialization is not supported and
//   will cause invalid instrumentation
public @interface SyntheticLocal {

	public enum Initialize {
		ALWAYS, NEVER, BEST_EFFORT 
	}
	
	// NOTE if you want to change names, you need to change SnippetParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	Initialize initialize() default(Initialize.ALWAYS);
}
