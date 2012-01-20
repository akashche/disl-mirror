package ch.usi.dag.dislserver;

public class JboratException extends Exception {

	private static final long serialVersionUID = 5272000884539359236L;

	public JboratException() {
		super();
	}

	public JboratException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public JboratException(String message, Throwable cause) {
		super(message, cause);
	}

	public JboratException(String message) {
		super(message);
	}

	public JboratException(Throwable cause) {
		super(cause);
	}
}
