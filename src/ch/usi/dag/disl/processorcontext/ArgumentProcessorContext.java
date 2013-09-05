package ch.usi.dag.disl.processorcontext;

/**
 * Allows accessing method arguments and apply argument processors.
 */
public interface ArgumentProcessorContext {
	
	/**
	 * Applies mentioned processor for method or call-site arguments.
	 * 
	 * @param argumentProcessor processor class to apply
	 * @param mode in which should be processor applied
	 */
	public void apply(Class<?> argumentProcessor, ArgumentProcessorMode mode);

	/**
	 * Returns the object on which is the processed method (arguments of that
	 * method) called. Returns null for static methods.
	 * 
	 * @param mode for which should be the object retrieved
	 */
	public Object getReceiver(ArgumentProcessorMode mode);

	/**
	 * Returns the object array composed from the method arguments. Note that
	 * primitive types will be boxed.
	 * 
	 * @param mode for which should be the argument array retrieved
	 */
	public Object[] getArgs(ArgumentProcessorMode mode);
}
