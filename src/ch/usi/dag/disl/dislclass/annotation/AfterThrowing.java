package ch.usi.dag.disl.dislclass.annotation;

import ch.usi.dag.disl.dislclass.snippet.marker.Marker;

public @interface AfterThrowing {
	
	// NOTE if you want to change names, you need to change SnippetParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	Class<? extends Marker> marker();
	String param() default ""; // cannot be null :(
	String scope();
	int order() default 100;
}
