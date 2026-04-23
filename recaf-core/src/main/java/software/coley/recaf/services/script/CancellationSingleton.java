package software.coley.recaf.services.script;

import software.coley.recaf.util.CancelSignal;

/**
 * Cancellation singleton.
 * <p>
 * Injected into generated scripts.
 * <b>Do not use in normal code</b>.
 *
 * @author xDark
 * @see InsertCancelSignalVisitor
 * @see GenerateResult#requestStop()
 */
public final class CancellationSingleton {
	private static volatile boolean shouldStop;

	private CancellationSingleton() {}

	/**
	 * Schedules cancellation of the script.
	 */
	public static void stop() {
		shouldStop = true;
	}

	/**
	 * Clears any prior cancellation request.
	 */
	public static void reset() {
		shouldStop = false;
	}

	/**
	 * Polls for cancellation.
	 *
	 * @throws CancelSignal
	 * 		If cancellation has been requested.
	 */
	public static void poll() {
		if (shouldStop)
			throw CancelSignal.get();
	}
}
