package ch.usi.dag.disl.dislclass.annotation;

import ch.usi.dag.disl.dislclass.snippet.marker.Marker;
import ch.usi.dag.disl.guard.SnippetGuard;

public @interface AfterReturning {

	// NOTE if you want to change names, you need to change SnippetParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	Class<? extends Marker> marker();
	String param() default ""; // cannot be null :(
	String scope();
	Class<? extends SnippetGuard> guard() default SnippetGuard.class; // cannot be null :(
	int order() default 100;
	// NOTE if the DiSL property disl.dynbypass is set to yes, dynamic bypass is
	// automatically enabled for each snippet
	boolean dynamicBypass() default false;
}
