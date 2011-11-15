package ch.usi.dag.disl.processor;

public abstract class Processor {
	
	/**
	 * Applies mentioned processor for method or call-site arguments
	 * 
	 * @param processorClass processor class to apply
	 * @param applyType mode in which should be processor applied
	 */
	public static void apply(Class<?> processorClass, ProcessorMode applyType) {
		
	}

	// TODO ! receiver - should it have apply type?, add description
	public static Object getReceiver(ProcessorMode applyType) {
		return null;
	}
}
