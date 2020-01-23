package me.xdark.recaf.jvm;

final class ExecutionCancelSignal extends Error {
	static final ExecutionCancelSignal INSTANCE = new ExecutionCancelSignal();

	private ExecutionCancelSignal() { }

	@Override
	public Throwable initCause(Throwable cause) {
		return this;
	}

	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
