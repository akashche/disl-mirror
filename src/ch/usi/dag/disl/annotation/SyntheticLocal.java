package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that a field is used for passing data between several snippets
 * inlined in the same method. Accesses to the field are translated to accesses
 * to a local variable within the method. By default, the local variable is
 * initialized to the assigned value or the default value of a corresponding
 * type. It is possible to disable the initialization using optional
 * "initialize" annotation parameter.
 * <p>
 * <b>Note:</b> Initialization can be done only within field definition. The
 * Java <code>static { ... }</code> construct is not supported for variable
 * initialization and could result in invalid instrumentation.
 * <p>
 * This annotation should be used with fields. The fields should be declared
 * {@code static} and, unless shared between multiple DiSL classes,
 * {@code private}.
 */
@Documented
@Target (ElementType.FIELD)
@Retention (RetentionPolicy.CLASS)
public @interface SyntheticLocal {

	public enum Initialize {
		ALWAYS, NEVER, BEST_EFFORT
	}

	// NOTE if you want to change names, you need to change
	// AbstractParser.SLAnnotaionData class

	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	/**
	 * Initialization mode of the synthetic local variable.
	 *
	 * Default value: Initialize.ALWAYS
	 */
	Initialize initialize() default (Initialize.ALWAYS);
}
