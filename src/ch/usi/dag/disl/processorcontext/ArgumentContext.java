package ch.usi.dag.disl.processorcontext;

/**
 * Allows to access information about particular argument.
 */
public interface ArgumentContext {

	/**
	 * Returns position of the processed argument
	 */
	public int getPosition();

	/**
	 * Returns type descriptor of the processed argument
	 */
	public String getTypeDescriptor();
	
	/**
	 * Returns total number of processed arguments
	 */
	public int getTotalCount();
}
