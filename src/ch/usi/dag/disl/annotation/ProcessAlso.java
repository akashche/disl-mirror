package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Extends the set of primitive integer types (except {@code long}) accepted
 * by an argument processor method:
 * <ul>
 * <li>for {@code int} argument processor, it allows to process also
 * {@code short}, {@code byte}, and {@code boolean} types;
 * <li>for {@code short} argument processor, it allows to process also
 * {@code byte}, and {@code boolean} types.
 * <li>for {@code byte} argument processor, it allows to process also
 * {@code boolean} type.
 * </ul>
 */
@Documented
@Target (ElementType.METHOD)
@Retention (RetentionPolicy.CLASS)
public @interface ProcessAlso {

	// TODO Consider support for {@code long} types.

	// NOTE if you want to change names, you need to change
	// ProcessorParser.ProcessAlsoAnnotationData class

	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	public enum Type {
		BOOLEAN, BYTE, SHORT
	}


	Type [] types();

}
