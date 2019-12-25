package my.exceptions;

public class ApplicationException extends Exception {

	private static final long serialVersionUID = 5884256128923807070L;

	public ApplicationException(Throwable t) {
		super(t);
	}

	public ApplicationException(String msg) {
		super(msg);
	}

}
