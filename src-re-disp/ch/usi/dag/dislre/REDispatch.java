package ch.usi.dag.dislre;

public class REDispatch {

	/**
	 * Announce start of an analysis transmission
	 *
	 * @param analysisID remote analysis id
	 * @param methodID remote method id
	 * 
	 * @return session id for consequent send
	 */
	// TODO re - allow strings with remote string cache support?
	public static native int analysisStart(int analysisMethodID);

	/**
	 * Announce end of an analysis transmission
	 */
	public static native void analysisEnd(int id);

	// allows transmitting types
	public static native void sendBoolean(int sid, boolean booleanToSend);
	public static native void sendByte(int sid, byte byteToSend);
	public static native void sendChar(int sid, char charToSend);
	public static native void sendShort(int sid, short shortToSend);
	public static native void sendInt(int sid, int intToSend);
	public static native void sendLong(int sid, long longToSend);
	public static native void sendFloatAsInt(int sid, int floatAsIntToSend);
	public static native void sendDoubleAsLong(int sid, long doubleAsLongToSend);
	public static native void sendString(int sid, String stringToSend);
	public static native void sendObject(int sid, Object objToSend);
	public static native void sendClass(int sid, Class<?> classToSend);
	
	// helper methods for sending float and double
	// for proper conversion, we would still need to call ...Bits methods
	// it is faster to call them here then call it from native code
	// faster would be to do it native code all - be my guest :)
	
	public static void sendFloat(int sid, float floatToSend) {

		sendFloatAsInt(sid, Float.floatToIntBits(floatToSend));
	}
	
	// helper method for sending double
	public static void sendDouble(int sid, double doubleToSend) {
		
		sendDoubleAsLong(sid, Double.doubleToLongBits(doubleToSend));
	}
}
