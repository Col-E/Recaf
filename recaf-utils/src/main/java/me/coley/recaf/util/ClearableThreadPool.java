package me.coley.recaf.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
		super(size, size, 5000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
				new ThreadFactory() {
					final AtomicInteger count = new AtomicInteger();
					final String format = name + " #%d";

					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r, String.format(format, count.getAndIncrement()));
						thread.setDaemon(daemon);
						return thread;
					}
				});
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
		List<Runnable> tasks = new ArrayList<>(activeThreads.keySet());
		for (Thread t : new ArrayList<>(activeThreads.values())) {
			try {
				t.stop();
			} catch (ThreadDeath death) {
				// Yeah yeah, we know this is a terrible idea.
			}
		}
		activeThreads.clear();
		return tasks;
	}

	/**
	 * Kills all current threads in the pool.
	 *
	 * @return Tasks of all threads that were running.
	 */
	@SuppressWarnings("deprecation")
	public synchronized List<Runnable> clearAndShutdown() {
		List<Runnable> runnables = super.shutdownNow();
		for (Thread t : activeThreads.values()) {
			t.stop();
		}
		activeThreads.clear();
		return runnables;
	}

	/**
	 * @return {@code true} when there are currently running threads in the pool.
	 */
	public synchronized boolean hasActiveThreads() {
		return !activeThreads.isEmpty();
	}
}