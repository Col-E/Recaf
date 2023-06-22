package software.coley.recaf.util;

/**
 * Special type of {@link Error} that signals that further execution must be stopped.
 *
 * @author xDark
 */
public final class CancelSignal extends Error {
	private static final CancelSignal INSTANCE = new CancelSignal(false);

	// Internal flag that *might* be useful for debugging
	// in the future.
	// Do NOT commit changes if this flag
	// is set to TRUE.
	private static final boolean DEBUG = false;

	/**
	 * Deny all constructions.
	 */
	private CancelSignal(boolean writableStackTrace) {
		super(null, null, false, writableStackTrace);
	}

	/**
	 * @return Cancellation signal.
	 */
	public static CancelSignal get() {
		if (DEBUG) {
			return new CancelSignal(true);
		}
		return INSTANCE;
	}
}
