package me.coley.recaf.simulation;

public abstract class SimulationException extends Exception {

	protected SimulationException(String message) {
		super(message);
	}

	protected SimulationException(String message, Throwable cause) {
		super(message, cause);
	}

	protected SimulationException(Throwable cause) {
		super(cause);
	}
}
