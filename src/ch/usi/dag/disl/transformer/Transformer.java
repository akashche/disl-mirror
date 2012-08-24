package ch.usi.dag.disl.transformer;

/**
 * Allows to transform a class before it is passed to DiSL. The transformer
 * class has to be specified in the instrumentation manifest.
 */
public interface Transformer {

	/**
	 * The transformation interface. The class to be transformed is passed as
	 * an argument and the transformed class is returned.
	 * 
	 * @param classfileBuffer class to be transformed
	 * @return transformed class
	 */
	byte[] transform(byte[] classfileBuffer) throws Exception;
	
	/**
	 * If this method returns true, not instrumented classes are still
	 * set as changed and returned by DiSL as modified. Otherwise, the DiSL will
	 * report class as unmodified. 
	 * 
	 * @return propagation flag
	 */
	boolean propagateUninstrumentedClasses();
}
