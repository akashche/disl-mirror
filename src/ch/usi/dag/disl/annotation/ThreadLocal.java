package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marks a field to be used for passing data between snippets inlined in
 * different methods and executing in the same thread. Accesses to the field are
 * translated to accesses to a thread-local variable.
 * <p>
 * By default, the thread local variable is always initialized the default value
 * of the corresponding type. If desired, the default value can be inherited
 * from the parent thread using the optional {@code inheritable} annotation
 * parameter.
 * <p>
 * This annotation should be used with fields.
 * <p>
 * The field should be declared {@code static} and, unless shared between
 * multiple DiSL classes, {@code private}.
 */
@Documented
@Target (ElementType.FIELD)
@Retention (RetentionPolicy.CLASS)
public @interface ThreadLocal {

	// NOTE if you want to change names, you need to change
	// ClassParser.TLAnnotationData class in startutil project

	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	/**
	 * If {@code true}, the thread-local variable will inherit default value
	 * from the parent thread. Default {@code false}.
	 */
	boolean inheritable() default false;

}
