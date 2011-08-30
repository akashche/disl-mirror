package ch.usi.dag.disl.exception;

public class StaticAnalysisException extends RuntimeException {

	private static final long serialVersionUID = -6364742721139705511L;

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
