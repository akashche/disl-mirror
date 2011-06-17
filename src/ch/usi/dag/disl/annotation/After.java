package ch.usi.dag.disl.annotation;

import ch.usi.dag.disl.snippet.marker.Marker;

public @interface After {
	
	// NOTE if you want to change names, you need to change AnnotationParser class
	Class<Marker> marker();
	String scope();
	int order() default 100;
}
