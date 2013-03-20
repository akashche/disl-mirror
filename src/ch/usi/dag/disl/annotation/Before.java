package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ch.usi.dag.disl.classcontext.ClassContext;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorContext;
import ch.usi.dag.disl.staticcontext.StaticContext;


/**
 * Marks DiSL snippet to be inserted before the marked code region. The snippet
 * will be executed before entering the marked code region.
 * <p>
 * NOTE: This is a general contract. The actual implementation depends on the
 * particular marker used with the snippet.
 * <p>
 * This annotation should be used with methods.
 * <p>
 * The snippet method must be {@code static}, must not return any value, and
 * must not throw any exceptions.
 * <p>
 * Supported method argument types include {@link StaticContext},
 * {@link DynamicContext}, {@link ClassContext}, and
 * {@link ArgumentProcessorContext}.
 */
@Documented
@Target (ElementType.METHOD)
@Retention (RetentionPolicy.CLASS)
public @interface Before {

	// NOTE if you want to change names, you need to change
	// SnippetParser.SnippetAnnotationData class

	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above

	/**
	 * Marker class. Determines the region of code where to apply the snippet.
	 */
	Class <? extends Marker> marker();


	/**
	 * Argument for the marker (as string).
	 * <p>
	 * Default value: ""
	 */
	String args() default "";


	/**
	 * Scope of the methods, where the snippet is applied.
	 * <p>
	 * @see ch.usi.dag.disl.scope package for more info about scoping language.
	 * <p>
	 * Default value: "*"
	 */
	String scope() default "*";


	/**
	 * The guard class defining if the snippet will be inlined in particular
	 * region or not.
	 * <p>
	 * Default value: void.class - means none
	 */
	Class <? extends Object> guard() default void.class;


	/**
	 * Determines snippet ordering. Smaller number indicates that snippet
	 * will be inlined closer to the marked code region.
	 * <p>
	 * Default value: 100
	 */
	int order() default 100;


	/**
	 * Advanced option. You can in general disable dynamic bypass on snippets,
	 * that are not using any other class.
	 * <p>
	 * NOTE: Usage of dynamic bypass is determined by the underlying
	 * instrumentation framework.
	 * <p>
	 * Default value: true
	 */
	boolean dynamicBypass() default true;

}
