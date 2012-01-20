package ch.usi.dag.disl.annotation;

import ch.usi.dag.disl.marker.Marker;

public @interface Before {
	
	// NOTE if you want to change names, you need to change 
	// SnippetParser.SnippetAnnotationData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	Class<? extends Marker> marker();
	String args() default ""; // cannot be null :(
	String scope() default "*";
	Class<? extends Object> guard() default Object.class; // cannot be null :(
	int order() default 100;
	// NOTE that activation of dynamic bypass is decided by the instrumentation
	// framework in first place
	boolean dynamicBypass() default true;
}
