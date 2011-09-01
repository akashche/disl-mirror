package ch.usi.dag.disl.dislclass.annotation;

public @interface ThreadLocal {

	// NOTE if you want to change names, you need to change 
	// ClassParser.TLAnnotationData class in startutil project
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	boolean inheritable() default(false);
}
