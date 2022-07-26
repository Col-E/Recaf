package me.coley.recaf.util.threading;

import java.util.concurrent.CompletableFuture;

/**
 * Utility to run a task after a delay with the option to continually push back execution of the task.
 *
 * @author Matt Coley
 */
public abstract class DelayedExecutor {
	private final long duration;
	private long lastReset;
	private CompletableFuture<?> future;

	/**
	 * @param duration
	 * 		Delay duration.
	 */
	public DelayedExecutor(long duration) {
		this.duration = duration;
		delay();
	}

	/**
	 * @return Delay duration.
	 */
	public long getDuration() {
		return duration;
	}

	/**
	 * Execute the action.
	 */
	protected abstract void execute();

	/**
	 * Start a background thread to run the task after {@link #getDuration() the delay}.
	 */
	public void start() {
		future = ThreadUtil.run(() -> {
			try {
				// Initial leeway delay
				Thread.sleep(100);
				while (true) {
					Thread.sleep(duration);
					// Check if a reset was triggered within the last duration.
					// If no reset was found, then run the action.
					// Otherwise, we'll wait again.
					long now = System.currentTimeMillis();
					long timeSinceLastReset = now - lastReset;
					if (timeSinceLastReset > duration) {
						execute();
						return;
					}
				}
			} catch (InterruptedException ignored) {
				// Interrupts indicate we cancelled the action
			}
		});
	}

	/**
	 * Cancel the task.
	 */
	public void cancel() {
		if (future != null)
			future.cancel(true);
	}

	/**
	 * Push back executing the task by {@link #getDuration() the delay duration}.
	 */
	public void delay() {
		if (future == null || future.isDone())
			start();
		lastReset = System.currentTimeMillis();
	}
}
