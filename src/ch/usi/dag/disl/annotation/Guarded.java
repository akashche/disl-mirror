package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used in {@link ArgumentProcessor} to guard specific methods.
 * <p>
 * This annotation should be used with methods.
 */
@Documented
@Target (ElementType.METHOD)
@Retention (RetentionPolicy.CLASS)
public @interface Guarded {

	// NOTE if you want to change names, you need to change
	// ProcessorParser.ProcMethodAnnotationData class

	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	/**
	 * The guard class defining if the processor method will be inlined or not.
	 */
	Class <? extends Object> guard();

}
