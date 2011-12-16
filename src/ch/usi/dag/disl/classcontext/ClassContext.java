package ch.usi.dag.disl.classcontext;

/**
 * Allows convert Strings to Class objects. The context is allowed in
 * snippets and argument processors
 */
public interface ClassContext {
	
	Class<?> asClass(String name); 
}
