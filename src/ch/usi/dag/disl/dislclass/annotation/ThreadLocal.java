package ch.usi.dag.disl.dislclass.annotation;

public @interface ThreadLocal {

	// NOTE if you want to change names, you need to change ClassParser class
	// in start subproject
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	boolean inheritable() default(false);
}
