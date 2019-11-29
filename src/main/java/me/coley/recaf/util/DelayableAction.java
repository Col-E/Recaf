package me.coley.recaf.util;

/**
 * Continually delay-able action.
 *
 * @author Matt
 */
public class DelayableAction extends Thread {
	private final long threshold;
	private final Runnable action;
	private long lastEdit;
	private boolean done;

	/**
	 * @param threshold
	 * 		Delay to wait until running the action.
	 * @param action
	 * 		Action to run.
	 */
	public DelayableAction(long threshold, Runnable action) {
		this.threshold = threshold;
		this.action = action;
	}

	@Override
	public void run() {
		// Keep delaying if needed
		while(System.currentTimeMillis() - lastEdit < threshold) {
			try {
				sleep(50);
			} catch(Exception ex) { /* ignored */ }
		}
		// Run action
		action.run();
		done = true;
	}

	/**
	 * Reset the clock for when to run the action.
	 */
	public void resetDelay() {
		this.lastEdit = System.currentTimeMillis();
	}

	/**
	 * @return {@code true} if the action has been executed.
	 */
	public boolean isDone() {
		return done;
	}
}