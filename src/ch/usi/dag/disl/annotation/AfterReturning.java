package ch.usi.dag.disl.annotation;

import ch.usi.dag.disl.marker.Marker;

/**
 * The AfterReturning annotation instructs DiSL to insert the snippet body after
 * the marked region. The snippet will be invoked after a normal exit of the
 * region.
 * 
 * NOTE: This is only general contract. It depends on particular marker how the
 * contract will be implemented.
 * 
 * This annotation should be used with methods.
 * 
 * The method should be static, not return any values and not throw any
 * exceptions.
 * 
 * Method argument can be StaticContext, DynamicContext, ClassContext and
 * ArgumentProcessorContext.
 */
public @interface AfterReturning {

	// NOTE if you want to change names, you need to change 
	// SnippetParser.SnippetAnnotationData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	/**
	 * Marker class defines a region where the snippet is applied.
	 */
	Class<? extends Marker> marker();
	
	/**
	 * Argument for the marker (as string).
	 * 
	 * Default value: ""
	 */
	String args() default ""; // cannot be null :(
	
	/**
	 * Scope of the methods, where the snippet is applied.
	 * 
	 * @see ch.usi.dag.disl.scope package for more info about scoping language.
	 * 
	 * Default value: "*"
	 */
	String scope() default "*";
	
	/**
	 * The guard class defining if the snippet will be inlined in particular
	 * region or not.
	 * 
	 * Default value: Object.class - means none
	 */
	Class<? extends Object> guard() default Object.class; // cannot be null :(
	
	/**
	 * Defines ordering of the snippets. Smaller number indicates that snippet
	 * will be inlined closer to the instrumented code.
	 * 
	 * Default value: 100
	 */
	int order() default 100;

	/**
	 * Advanced option. You can in general disable dynamic bypass on snippets,
	 * that are not using any other class.
	 * 
	 * NOTE Usage of dynamic bypass is determined by the underlying
	 * instrumentation framework.
	 * 
	 * Default value: true
	 */
	boolean dynamicBypass() default true;
}
