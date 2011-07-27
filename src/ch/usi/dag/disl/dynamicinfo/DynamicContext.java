package ch.usi.dag.disl.dynamicinfo;

abstract public class DynamicContext {

	/**
	 * Returns value on application stack at runtime.
	 * 
	 * @param distance position from the top of the stack.
	 * 			0 returns top of the stack.
	 * 			Doubles and longs are counted as distance difference 1.
	 * @param valueType type of the accessed value
	 */
	<T> T getStackValue(int distance, Class<T> valueType) {
		return null;
	}
	
	/**
	 * Returns value of method argument at given position at runtime.
	 * 
	 * @param index argument position.
	 * 			0 returns this if applicable.
	 * 			Doubles and longs are counted as distance difference 1.
	 * @param valueType type of the accessed argument
	 */
	<T> T getMethodArgumentValue(int index, Class<T> valueType) {
		return null;
	}
	
	/**
	 * Returns value of local variable with given index at runtime.
	 * 
	 * @param index argument position.
	 * 			You have to know the exact index of the accessed local variable.
	 * 			Doubles and longs are counted as distance difference 2.
	 * @param valueType type of the accessed argument
	 */
	<T> T getLocalVariableValue(int index, Class<T> valueType) {
		return null;
	}
}
