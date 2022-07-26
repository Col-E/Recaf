package me.coley.recaf.util.threading;

/**
 * {@link Runnable} implementation of {@link DelayedExecutor}.
 *
 * @author Matt Coley
 */
public class DelayedRunnable extends DelayedExecutor {
	private final Runnable action;

	/**
	 * @param duration
	 * 		Delay duration.
	 * @param action
	 * 		Task to run after the delay.
	 */
	public DelayedRunnable(long duration, Runnable action) {
		super(duration);
		this.action = action;
	}

	@Override
	protected void execute() {
		action.run();
	}
}
