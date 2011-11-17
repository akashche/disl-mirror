package ch.usi.dag.disl.processor;

public interface ArgumentContext {

	/**
	 * Returns position of the processed argument
	 */
	public int position();

	/**
	 * Returns type of the processed argument
	 */
	public String type();
	
	/**
	 * Returns total number of processed arguments
	 */
	public int totalCount();
}
