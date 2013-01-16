package ch.usi.dag.dislre.jb;

import java.io.ByteArrayOutputStream;

// This class allows to write in the middle of the stream by setting the count
// value of the underlying ByteArrayInputStream to specific values
public class UglyByteArrayOutputStream extends ByteArrayOutputStream {

	protected int realCount = -1;
	
	public UglyByteArrayOutputStream() {
		super();
	}

	public UglyByteArrayOutputStream(int size) {
		super(size);
	}

	// until the resetPosition is called, buffer is treated as smaller
	public void setPosition(int pos) {
		
		// wrong argument, position can be set only in filled space
		if(pos > count) {
			throw new IllegalArgumentException(
					"Position is higher then (filled) size of the buffer");
		}
		
		realCount = count;
		count = pos;
	}
	
	public void resetPosition() {
		
		// nothing to reset
		if(realCount < count) {
			return;
		}
		
		count = realCount;
		realCount = -1;
	}
	
	public byte[] getBuffer() {
		return buf;
	}
}
