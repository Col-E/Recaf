package me.coley.recaf.ssvm;

import dev.xdark.ssvm.value.Value;

/**
 * Wrapper around a VM return value, or an exception if the VM could not execute.
 *
 * @author Matt Coley
 */
public class VmRunResult {
	private Throwable exception;
	private Value value;

	/**
	 * @param value
	 * 		Execution return value.
	 */
	public VmRunResult(Value value) {
		this.value = value;
	}

	/**
	 * @param exception
	 * 		Execution failure.
	 */
	public VmRunResult(Throwable exception) {
		this.exception = exception;
	}

	/**
	 * @return Execution return value.
	 */
	public Value getValue() {
		return value;
	}

	/**
	 * @return Execution failure.
	 */
	public Throwable getException() {
		return exception;
	}

	/**
	 * @return {@code true} when there is an {@link #getException() error}.
	 */
	public boolean hasError() {
		return exception != null;
	}
}
