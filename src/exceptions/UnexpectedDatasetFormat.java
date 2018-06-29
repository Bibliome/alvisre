package exceptions;

public class UnexpectedDatasetFormat extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public UnexpectedDatasetFormat(String message) {
		super(message);
	}

	public UnexpectedDatasetFormat(String message, Throwable caca) {
		super(message, caca);
	}
}