package ch.usi.dag.dislre.jb;

import ch.usi.dag.dislre.REDispatch;

public class REDispatchJ {

	// TODO ! JB - this is not unique in the case of multiple classloaders
	static final ThreadLocal<JBBuffer> buffer = new ThreadLocal<JBBuffer>() {
		
		@Override
		protected JBBuffer initialValue() {
			return JBBufferPool.getEmpty();
		}
	};
	
	/**
	 * Register method and receive id for this transmission 
	 * 
	 * @param analysisMethodDesc
	 * @return
	 */
	public static short registerMethod(String analysisMethodDesc) {
		return REDispatch.registerMethod(analysisMethodDesc);
	}
	
	/**
	 * Announce start of an analysis transmission
	 *
	 * @param analysisMethodDesc remote analysis method id
	 */
	public static void analysisStart(short analysisMethodId) {
		buffer.get().analysisStart(analysisMethodId);
	}
	
	/**
	 * Announce start of an analysis transmission with total ordering (among
	 * several threads) under the same orderingId
	 *
	 * @param analysisMethodId remote analysis method id
	 * @param orderingId analyses with the same orderingId are guaranteed to
	 *                   be ordered. Only non-negative values are valid.
	 */
	public static void analysisStart(short analysisMethodId,
			byte orderingId) {
		// TODO ! JB
	}

	/**
	 * Announce end of an analysis transmission
	 */
	public static void analysisEnd() {
		
		JBBuffer buff = buffer.get();
		
		// submit full buffer and obtain empty one
		if(buff.analysisEnd()) {
			
			JBBufferPool.putFull(buff);
			buffer.set(JBBufferPool.getEmpty());
		}
	}

	// allows transmitting types
	public static void sendBoolean(boolean booleanToSend) {
		buffer.get().putBoolean(booleanToSend);
	}
	
	public static void sendByte(byte byteToSend) {
		buffer.get().putByte(byteToSend);
	}
	
	public static void sendChar(char charToSend) {
		buffer.get().putChar(charToSend);
	}
	
	public static void sendShort(short shortToSend) {
		buffer.get().putShort(shortToSend);
	}
	
	public static void sendInt(int intToSend) {
		buffer.get().putInt(intToSend);
	}
	
	public static void sendLong(long longToSend) {
		buffer.get().putLong(longToSend);
	}
	
	public static void sendFloat(int floatToSend) {
		buffer.get().putFloat(floatToSend);
	}
	
	public static void sendDouble(long doubleToSend) {
		buffer.get().putDouble(doubleToSend);
	}
	
	public static void sendString(String stringToSend) {
		// TODO ! JB
	}
	
	public static void sendObject(Object objToSend) {
		buffer.get().putObject(objToSend);
	}
	
	public static void sendClass(Class<?> classToSend) {
		// TODO ! JB
	}
}
