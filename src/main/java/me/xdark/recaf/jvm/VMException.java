package me.xdark.recaf.jvm;

public class VMException extends Exception {

	public VMException() { }

	public VMException(String message) {
		super(message);
	}

	public VMException(String message, Throwable cause) {
		super(message, cause);
	}

	public VMException(Throwable cause) {
		super(cause);
	}

	public VMException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
