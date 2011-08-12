package ch.usi.dag.disl.annotation;

import ch.usi.dag.disl.snippet.marker.Marker;

public @interface AfterReturning {

	// NOTE if you want to change names, you need to change SnippetParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	Class<? extends Marker> marker();
	String param() default ""; // cannot be null :(
	String scope();
	int order() default 100;
}
