package me.xdark.recaf.jvm;

public final class InvalidBytecodeException extends VMException {

	public InvalidBytecodeException(String message) {
		super(message);
	}

	public InvalidBytecodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidBytecodeException(Throwable cause) {
		super(cause);
	}
}
