package ch.usi.dag.dislre;

public class REDispatch {

	/**
	 * Register method and receive id for this transmission 
	 * 
	 * @param analysisMethodDesc
	 * @return
	 */
	public static native short registerMethod(String analysisMethodDesc);
	
	/**
	 * Announce start of an analysis transmission
	 *
	 * @param analysisMethodDesc remote analysis method id
	 */
	public static native void analysisStart(short analysisMethodId);
	
	/**
	 * Announce start of an analysis transmission with total ordering (among
	 * several threads) under the same orderingId
	 *
	 * @param analysisMethodId remote analysis method id
	 * @param orderingId analyses with the same orderingId are guaranteed to
	 *                   be ordered. Only non-negative values are valid.
	 */
	public static native void analysisStart(short analysisMethodId,
			byte orderingId);

	/**
	 * Announce end of an analysis transmission
	 */
	public static native void analysisEnd();

	// allows transmitting types
	public static native void sendBoolean(boolean booleanToSend);
	public static native void sendByte(byte byteToSend);
	public static native void sendChar(char charToSend);
	public static native void sendShort(short shortToSend);
	public static native void sendInt(int intToSend);
	public static native void sendLong(long longToSend);
	public static native void sendFloatAsInt(int floatAsIntToSend);
	public static native void sendDoubleAsLong(long doubleAsLongToSend);
	public static native void sendString(String stringToSend);
	public static native void sendObject(Object objToSend);
	public static native void sendClass(Class<?> classToSend);
	
	// helper methods for sending float and double
	// for proper conversion, we would still need to call ...Bits methods
	// it is faster to call them here then call it from native code
	// faster would be to do it native code all - be my guest :)
	
	public static void sendFloat(float floatToSend) {

		sendFloatAsInt(Float.floatToIntBits(floatToSend));
	}
	
	// helper method for sending double
	public static void sendDouble(double doubleToSend) {
		
		sendDoubleAsLong(Double.doubleToLongBits(doubleToSend));
	}
	
	// TODO re - basic type array support
	//  - send length + all values in for cycle
}
