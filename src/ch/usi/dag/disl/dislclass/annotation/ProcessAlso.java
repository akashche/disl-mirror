package ch.usi.dag.disl.dislclass.annotation;


public @interface ProcessAlso {

	// NOTE if you want to change names, you need to change 
	// ProcessorParser.ProcessAlsoAnnotationData class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from class mentioned above
	
	public enum Type {
		BOOLEAN,
		BYTE,
		SHORT
	}
	
	Type[] types();
}
