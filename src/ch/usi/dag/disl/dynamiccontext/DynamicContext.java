package ch.usi.dag.disl.dynamiccontext;

/**
 * Dynamic context provides access to dynamic information of the snippet. 
 */
public interface DynamicContext {

	/**
	 * Returns value on application stack at runtime.
	 * 
	 * @param distance position from the top of the stack.
	 * 			0 returns top of the stack.
	 * 			Doubles and longs are counted as distance difference 1.
	 * @param valueType type of the accessed value
	 */
	public <T> T getStackValue(int distance, Class<T> valueType);
	
	/**
	 * Returns value of local variable with given index at runtime.
	 * 
	 * @param index argument position.
	 * 			You have to know the exact index of the accessed local variable.
	 * 			Doubles and longs are counted as distance difference 2.
	 * @param valueType type of the accessed argument
	 */
	public <T> T getLocalVariableValue(int index, Class<T> valueType);
	
	/**
	 * Returns value of this object for dynamic method null for static method.
	 */
	public Object getThis();
	
	/**
	 * Returns value of method argument at given position at runtime.
	 * 
	 * @param index argument position.
	 * 			0 returns first argument.
	 * 			Doubles and longs are counted as distance difference 1.
	 * @param valueType type of the accessed argument
	 */
	public <T> T getMethodArgumentValue(int index, Class<T> valueType);

	/**
	 * Returns the exception reference when applying @After or @AfterThrowing,
	 * null otherwise.
	 */
	public Throwable getException();
}
