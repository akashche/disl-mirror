package ch.usi.dag.dislre;

public class REDispatch {

	/**
	 * Announce start of an analysis transmission
	 *
	 * @param analysisID remote analysis id
	 * @param methodID remote method id
	 */
	// TODO re - allow strings with remote string cache support?
	public static native void analysisStart(int analysisMethodID);

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
	public static native void sendFloat(float floatToSend);
	public static native void sendDouble(double doubleToSend);
	public static native void sendString(String stringToSend);
	public static native void sendObject(Object objToSend);
	public static native void sendClass(Class<?> classToSend);
}
