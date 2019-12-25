package my.exceptions;

public class ApplicationRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -3145228350162705742L;

	public ApplicationRuntimeException(String msg) {
		super(msg);
	}

	public ApplicationRuntimeException(Throwable t) {
		super(t);
	}

	public ApplicationRuntimeException(String msg, Throwable t) {
		super(msg, t);
	}
}
