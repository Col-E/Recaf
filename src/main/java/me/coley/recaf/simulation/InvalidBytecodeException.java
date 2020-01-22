package me.coley.recaf.simulation;

public final class InvalidBytecodeException extends SimulationException {

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
