package ch.usi.dag.disl.annotation;

/**
 * Indicates, that field is used for data passing between several snippets
 * inlined into one method. The field is translated into local variable within
 * a method. The local variable is by default always initialized to the assigned
 * value or the default value of a corresponding type. It is possible to disable
 * the  initialization using optional "initialize" annotation parameter.
 * 
 * NOTE: Initialization can be done only within field definition.
 * Java static { } construct for initialization is not supported and
 * could cause invalid instrumentation.
 * 
 * This annotation should be used with fields.
 * 
 * Field should be declared as static, and if not shared between multiple DiSL
 * classes, also private.
 */
public @interface SyntheticLocal {

	public enum Initialize {
		ALWAYS, NEVER, BEST_EFFORT 
	}
	
	// NOTE if you want to change names, you need to change 
	// AbstractParser.SLAnnotaionData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	/**
	 * Initialization mode of the SyntheticLocal variable.
	 * 
	 * Default value: Initialize.ALWAYS
	 */
	Initialize initialize() default(Initialize.ALWAYS);
}
