package software.coley.recaf.util.analysis.eval;

/**
 * Simulated evaluator clock.
 *
 * @author Matt Coley
 */
public class VirtualClock {
	private long nanos;

	/**
	 * @return Current virtual time in nanoseconds.
	 */
	public long nanos() {
		return nanos;
	}

	/**
	 * @return Current virtual time in milliseconds.
	 */
	public long millis() {
		return nanos / 1_000_000L;
	}

	/**
	 * @param deltaNanos
	 * 		Non-negative virtual duration to advance the clock by.
	 */
	public void advance(long deltaNanos) {
		if (deltaNanos < 0)
			throw new IllegalArgumentException("Virtual time cannot move backwards");
		long remaining = Long.MAX_VALUE - nanos;
		nanos = deltaNanos >= remaining ? Long.MAX_VALUE : nanos + deltaNanos;
	}
}
