package ch.usi.dag.disl.dynamicinfo;

public interface DynamicContext {

	/**
	 * Returns value on application stack at runtime.
	 * 
	 * @param distance position from the top of the stack.
	 * 			0 returns top of the stack.
	 * 			Doubles and longs are counted as distance difference 2.
	 * @param valueType type of the accessed value
	 */
	public <T> T stackValue(int distance, Class<T> valueType);
	
	/**
	 * Returns value of local variable with given index at runtime.
	 * 
	 * @param index argument position.
	 * 			You have to know the exact index of the accessed local variable.
	 * 			Doubles and longs are counted as distance difference 2.
	 * @param valueType type of the accessed argument
	 */
	public <T> T localVariableValue(int index, Class<T> valueType);
	
	/**
	 * Returns value of this object for dynamic method null for static method.
	 */
	public Object thisValue();
	
	/**
	 * Returns value of method argument at given position at runtime.
	 * 
	 * @param index argument position.
	 * 			0 returns this if applicable, null otherwise.
	 * 			Doubles and longs are counted as distance difference 1.
	 * @param valueType type of the accessed argument
	 */
	public <T> T methodArgumentValue(int index, Class<T> valueType);
}
