package ch.usi.dag.disl.exception;

public class ArgProcessorParserException extends ParserException {

    private static final long serialVersionUID = -7463716384020447033L;

    public ArgProcessorParserException() {
        super();
    }

    public ArgProcessorParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArgProcessorParserException(String message) {
        super(message);
    }

    public ArgProcessorParserException(Throwable cause) {
        super(cause);
    }
}
