package ch.usi.dag.disl.exception;

public class StaticInfoException extends DiSLException {

	private static final long serialVersionUID = 8195611687932799195L;

	public StaticInfoException() {
		super();
	}

	public StaticInfoException(String message, Throwable cause) {
		super(message, cause);
	}

	public StaticInfoException(String message) {
		super(message);
	}

	public StaticInfoException(Throwable cause) {
		super(cause);
	}
}
