package me.coley.recaf.util;

/**
 * Continually delay-able action.
 *
 * @author Matt
 */
public class DelayableAction extends Thread {
	private final Object lock = new Object();
	private final long threshold;
	private final Runnable action;
	private long lastEdit;

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
		synchronized (this.lock) {
			while (true) {
				long timeout = System.currentTimeMillis() + threshold;
				if (timeout > lastEdit) {
					long toWait = timeout - lastEdit;
					try {
						this.lock.wait(toWait);
					} catch (InterruptedException e) {/* ignored */}
					break;
				}
			}
		}
		// Run action
		action.run();
	}

	/**
	 * Reset the clock for when to run the action.
	 */
	public void resetDelay() {
		this.lastEdit = System.currentTimeMillis();
		synchronized (this.lock) {
			this.lock.notify();
		}
	}

	/**
	 * @return {@code true} if the action has been executed.
	 */
	public boolean isDone() {
		return getState() == State.TERMINATED;
	}
}