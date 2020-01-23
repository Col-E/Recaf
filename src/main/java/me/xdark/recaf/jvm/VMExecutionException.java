package me.xdark.recaf.jvm;

public final class VMExecutionException extends VMException {

	protected VMExecutionException(String message) {
		super(message);
	}

	protected VMExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	protected VMExecutionException(Throwable cause) {
		super(cause);
	}
}
