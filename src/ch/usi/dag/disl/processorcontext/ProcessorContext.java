package ch.usi.dag.disl.processorcontext;

public interface ProcessorContext {
	
	/**
	 * Applies mentioned processor for method or call-site arguments
	 * 
	 * @param processorClass processor class to apply
	 * @param mode in which should be processor applied
	 */
	public void apply(Class<?> processorClass, ProcessorMode mode);

	// TODO ! processor - add support
	/**
	 * Returns the object on which is the processed method (arguments of that
	 * method) called.
	 * 
	 * @param mode for which should be the object retrieved
	 */
	public Object getReceiver(ProcessorMode mode);
	
	// TODO ! processor - add support
	/**
	 * Returns the object array composed from the method arguments. Note that
	 * primitive types will be boxed.
	 * 
	 * @param mode for which should be the argument array retrieved
	 */
	public Object[] getArgs(ProcessorMode mode);
}
