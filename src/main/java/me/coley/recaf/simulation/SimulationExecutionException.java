package me.coley.recaf.simulation;

public final class SimulationExecutionException extends SimulationException {

	protected SimulationExecutionException(String message) {
		super(message);
	}

	protected SimulationExecutionException(String message, Throwable cause) {
		super(message, cause);
	}

	protected SimulationExecutionException(Throwable cause) {
		super(cause);
	}
}
