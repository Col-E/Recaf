package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * A batch implementation that directly runs provided tasks on the calling thread.
 *
 * @author Matt Coley
 */
public class DirectBatch implements Batch {
	private final List<Runnable> tasks = new ArrayList<>();

	@Override
	public void add(@Nonnull Runnable runnable) {
		synchronized (tasks) {
			tasks.add(runnable);
		}
	}

	@Override
	public void clear() {
		synchronized (tasks) {
			tasks.clear();
		}
	}

	@Override
	public void removeOldest() {
		synchronized (tasks) {
			tasks.removeFirst();
		}
	}

	@Override
	public void removeNewest() {
		synchronized (tasks) {
			tasks.removeLast();
		}
	}

	@Override
	public void execute() {
		List<Runnable> tasksCopy;
		synchronized (tasks) {
			tasksCopy = new ArrayList<>(tasks);
			tasks.clear();
		}
		for (Runnable task : tasksCopy)
			task.run();
	}

	@Override
	public void executeOldest() {
		Runnable task;
		synchronized (tasks) {
			if (tasks.isEmpty())
				return;
			task = tasks.removeFirst();
		}
		task.run();
	}

	@Override
	public void executeNewest() {
		Runnable task;
		synchronized (tasks) {
			if (tasks.isEmpty())
				return;
			task = tasks.removeLast();
		}
		task.run();
	}

	@Override
	public boolean isEmpty() {
		synchronized (tasks) {
			return tasks.isEmpty();
		}
	}
}