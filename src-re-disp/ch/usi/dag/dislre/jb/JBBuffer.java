package ch.usi.dag.dislre.jb;

import java.io.DataOutputStream;
import java.io.IOException;

// goal of this class is to mimic the buffering of the native agent
public final class JBBuffer {

	// TODO ! JB - replace with extensible arrays and number of analysis
	private static final int ONE_K = 1024;
	private static final int THRESHOLD = 50 * ONE_K;
	private static final int MAX_SIZE = THRESHOLD + 2 * ONE_K;

	// TODO ! JB - add constant from server
	private static final byte ANALYSIS_MSG_NUM = 1;

	private Long ownerThread;

	private UglyByteArrayOutputStream uos = new UglyByteArrayOutputStream(
			MAX_SIZE);
	private DataOutputStream dos = new DataOutputStream(uos);

	private int requestCount;
	private int requestCountPos;

	private int requestLenPos;

	private TagBuffer objectTB = new TagBuffer(MAX_SIZE);

	public void init() {
		ownerThread = Thread.currentThread().getId();
	}

	public void reset() {

		uos.reset();
		objectTB.reset();

		ownerThread = null;
		
		requestCount = 0;
    	requestCountPos = -1;
    	requestLenPos = -1;
	}

	public long getOwnerThread() {
		return ownerThread;
	}

	public byte[] getDataAsArray() {
		return uos.getBuffer();
	}

	public int sizeInBytes() {
		return uos.size();
	}

	public TagBuffer getObjectTB() {
		return objectTB;
	}

	public void analysisStart(short analysisMethodId) {

		// initialization (see below) should not be in init() because sometimes
		// we need really empty buffer
		
		// initialize buffer
		if (requestCount == 0) {

			// msg type
			putByte(ANALYSIS_MSG_NUM);

			// TODO ! JB - this number is non-negative - problem with total
			// order buffers
			// thread id
			putLong(ownerThread);

			requestCountPos = sizeInBytes();

			// request count space init
			putInt(requestCount);
		}

		// analysis method id
		putShort(analysisMethodId);

		requestLenPos = sizeInBytes();

		// request len space init
		putShort((short) 0);
	}

	// indicates whether buffer is full
	public boolean analysisEnd() {

		// update the length of analysis request
		int requestLen = sizeInBytes() - requestLenPos
				- (Short.SIZE / Byte.SIZE);
		uos.setPosition(requestLenPos);
		putShort((short) requestLen);
		uos.resetPosition();

		++requestCount;

		uos.setPosition(requestCountPos);
		putInt(requestCount);
		uos.resetPosition();

		// send the buffer
		if (sizeInBytes() > THRESHOLD) {
			return true;
		}

		return false;
	}

	public void putBoolean(boolean toPut) {
		try {
			dos.writeBoolean(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putByte(byte toPut) {
		try {
			dos.writeByte(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putChar(char toPut) {
		try {
			dos.writeChar(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putShort(short toPut) {
		try {
			dos.writeShort(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putInt(int toPut) {
		try {
			dos.writeInt(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putLong(long toPut) {
		try {
			dos.writeLong(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void putFloat(int toPut) {
		try {
			dos.writeFloat(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public void putDouble(long toPut) {
		try {
			dos.writeDouble(toPut);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public void putObject(Object toPut) {

		objectTB.add(toPut, sizeInBytes());

		// net reference type is long
		putLong(0);
	}
}
