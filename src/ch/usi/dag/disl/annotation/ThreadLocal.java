package ch.usi.dag.disl.annotation;

/**
 * Indicates, that field is used for data passing between several snippets
 * The field is translated into thread local variable. The thread local
 * variable is by default always initialized the default value of a
 * corresponding type. The default value can be inherited from a parent thread
 * using optional inheritable annotation parameter.
 * <br>
 * <br>
 * This annotation should be used with fields.
 * <br>
 * Field should be declared as static, and if not shared between multiple DiSL
 * classes, also private.
 */
public @interface ThreadLocal {

	// NOTE if you want to change names, you need to change 
	// ClassParser.TLAnnotationData class in startutil project
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	/**
	 * Indicates, weather is the default value inherited from a parent thread.
	 * 
	 * Default value: false
	 */
	boolean inheritable() default(false);
}
