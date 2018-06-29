package exceptions;

public class ProcessException extends RuntimeException{

	private static final long serialVersionUID = 1L;

	public ProcessException() {
		super();
	}

	public ProcessException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProcessException(String message) {
		super(message);
	}

	public ProcessException(Throwable cause) {
		super(cause);
	}

}
