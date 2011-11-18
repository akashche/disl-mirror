package ch.usi.dag.disl.processor;

public interface ArgumentContext {

	/**
	 * Returns position of the processed argument
	 */
	public int position();

	/**
	 * Returns type descriptor of the processed argument
	 */
	public String typeDescriptor();
	
	/**
	 * Returns total number of processed arguments
	 */
	public int totalCount();
}
