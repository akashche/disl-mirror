package ch.usi.dag.disl.exception;

public class StaticAnalysisException extends DiSLException {

	private static final long serialVersionUID = 8195611687932799195L;

	public StaticAnalysisException() {
		super();
	}

	public StaticAnalysisException(String message, Throwable cause) {
		super(message, cause);
	}

	public StaticAnalysisException(String message) {
		super(message);
	}

	public StaticAnalysisException(Throwable cause) {
		super(cause);
	}
}
