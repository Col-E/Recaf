package me.xdark.recaf.jvm;

public final class SimulationExecutionException extends VMException {

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
