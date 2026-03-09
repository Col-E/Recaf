package software.coley.recaf.services.script;

import software.coley.recaf.util.CancelSignal;

/**
 * Cancellation singleton.
 * Injected into generated scripts.
 * Do not use in normal code.
 *
 * @author xDark
 */
public final class CancellationSingleton {
	private static volatile boolean shouldStop;

	private CancellationSingleton() {
	}

	// Names and descriptors are known to the visitor/runtime.
	// See InsertCancelSignalVisitor.
	// See GenerateResult.
	public static void stop() {
		shouldStop = true;
	}

	public static void poll() {
		if (shouldStop) {
			throw CancelSignal.get();
		}
	}
}
