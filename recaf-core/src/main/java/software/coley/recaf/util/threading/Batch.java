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
	 * Run the oldest added task. This will remove the task once executed.
	 */
	void executeOldest();

	/**
	 * Run the newest added task. This will remove the task once executed.
	 */
	void executeNewest();

	/**
	 * @param runnable
	 * 		Task to execute.
	 */
	void add(@Nonnull Runnable runnable);

	/**
	 * Removes all tasks.
	 */
	void clear();

	/**
	 * Removes the oldest task.
	 */
	void removeOldest();

	/**
	 * Removes the newest task.
	 */
	void removeNewest();

	/**
	 * @return {@code true} when there are no tasks in the batch.
	 */
	boolean isEmpty();
}
