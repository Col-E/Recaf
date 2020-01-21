package me.coley.recaf.simulation;

public final class InvalidBytecodeException extends SimulationException {

	protected InvalidBytecodeException(String message) {
		super(message);
	}

	protected InvalidBytecodeException(String message, Throwable cause) {
		super(message, cause);
	}

	protected InvalidBytecodeException(Throwable cause) {
		super(cause);
	}
}
