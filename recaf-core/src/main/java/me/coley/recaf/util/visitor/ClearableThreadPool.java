package me.coley.recaf.util.visitor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Do not use this for anything that isn't purely <i>"background work"</i>.
 * Killing threads outright is usually a bad idea, but sometimes its necessary.
 *
 * @author Matt Coley
 */
public class ClearableThreadPool extends ThreadPoolExecutor {
	private final Map<Runnable, Thread> activeThreads = new HashMap<>();

	/**
	 * @param size
	 * 		Pool size.
	 * @param daemon
	 * 		Flag for if threads should be background threads and not hold up the VM on shutdown.
	 * @param name
	 * 		Thread name prefix.
	 */
	public ClearableThreadPool(int size, boolean daemon, String name) {
		super(size, size, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
				new ThreadFactoryBuilder()
						.setDaemon(daemon)
						.setNameFormat(name + " #%d")
						.build());
	}

	@Override
	protected synchronized void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		activeThreads.put(r, t);
	}

	@Override
	protected synchronized void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		activeThreads.remove(r);
	}

	/**
	 * Kills all current threads in the pool.
	 *
	 * @return Tasks of all threads that were running.
	 */
	@SuppressWarnings("deprecation")
	public synchronized List<Runnable> clear() {
		List<Runnable> runnables = super.shutdownNow();
		for (Thread t : activeThreads.values()) {
			t.stop();
		}
		return runnables;
	}

	/**
	 * @return {@code true} when there are currently running threads in the pool.
	 */
	public synchronized boolean hasActiveThreads() {
		return !activeThreads.isEmpty();
	}
}