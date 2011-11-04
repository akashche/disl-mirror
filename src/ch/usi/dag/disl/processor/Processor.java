package ch.usi.dag.disl.processor;

public abstract class Processor {
	
	/**
	 * Applies mentioned processor for method or call-site arguments
	 * 
	 * @param processorClass processor class to apply
	 * @param applyType mode in which should be processor applied
	 */
	public static void apply(Class<?> processorClass, ProcessorApplyType applyType) {
		
	}

	// TODO ! reciver - should it have apply type?, add desc
	public static Object getReceiver(ProcessorApplyType applyType) {
		return null;
	}
}
