package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

/**
 * Outlines a model to wrap multiple tasks into a single execution.
 *
 * @author Matt Coley
 */
public interface Batch {
	/**
	 * Run all registered tasks. This will remove all executed tasks once completed.
	 */
	void execute();

	/**
	 * @param runnable
	 * 		Task to execute.
	 */
	void add(@Nonnull Runnable runnable);

	/**
	 * Clears tasks.
	 */
	void clear();
}
