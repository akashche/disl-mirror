package ch.usi.dag.disl.processor;

public abstract class Processor {
	
	/**
	 * Applies mentioned processor for method or call-site arguments
	 * 
	 * @param processorClass processor class to apply
	 * @param mode in which should be processor applied
	 */
	public static void apply(Class<?> processorClass, ProcessorMode mode) {
		
	}

	// TODO ! processor - add support
	/**
	 * Returns the object on which is the processed method (arguments of that
	 * method) called.
	 * 
	 * @param mode for which should be the object retrieved
	 */
	public static Object getReceiver(ProcessorMode mode) {
		return null;
	}
	
	// TODO ! processor - add support
	/**
	 * Returns the object array composed from the method arguments. Note that
	 * primitive types will be boxed.
	 * 
	 * @param mode for which should be the argument array retrieved
	 */
	public static Object[] getArgs(ProcessorMode mode) {
		return null;
	}
}
