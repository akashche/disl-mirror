package ch.usi.dag.dislserver;



public final class Message {

	private final int __flags;

	private final byte [] __control;

	private final byte [] __classCode;

	//

	public Message (
		final int flags, final byte [] control, final byte [] classCode
	) {
		__flags = flags;
		__control = control;
		__classCode = classCode;
	}

	//

	public int getFlags () {
		return __flags;
	}


	public byte [] getControl () {
		return __control;
	}


	public byte [] getClassCode () {
		return __classCode;
	}

	//

	public boolean isShutdown () {
		return (__control.length == 0) && (__classCode.length == 0);
	}

}
