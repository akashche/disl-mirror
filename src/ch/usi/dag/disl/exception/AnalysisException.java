package ch.usi.dag.disl.exception;

public class AnalysisException extends DiSLException {

	private static final long serialVersionUID = 8195611687932799195L;

	public AnalysisException() {
		super();
	}

	public AnalysisException(String message, Throwable cause) {
		super(message, cause);
	}

	public AnalysisException(String message) {
		super(message);
	}

	public AnalysisException(Throwable cause) {
		super(cause);
	}
}
