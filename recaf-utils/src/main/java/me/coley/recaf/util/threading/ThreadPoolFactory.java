package me.coley.recaf.util.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Wrapper for {@link ExecutorService} with easier inline configuration.
 *
 * @author Matt Coley
 */
public class ThreadPoolFactory {
	private static final int MAX = Runtime.getRuntime().availableProcessors();

	/**
	 * @param name
	 * 		Thread pool name.
	 *
	 * @return {@link Executors#newFixedThreadPool(int)}.
	 */
	public static ExecutorService newFixedThreadPool(String name) {
		return newFixedThreadPool(name, true);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newFixedThreadPool(int)}.
	 */
	public static ExecutorService newFixedThreadPool(String name, boolean daemon) {
		return newFixedThreadPool(name, MAX, daemon);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param size
	 * 		Thread pool size.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newFixedThreadPool(int)}.
	 */
	public static ExecutorService newFixedThreadPool(String name, int size, boolean daemon) {
		return Executors.newFixedThreadPool(Math.min(MAX, size), new FactoryImpl(name, daemon));
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 *
	 * @return {@link Executors#newCachedThreadPool()}.
	 */
	public static ExecutorService newCachedThreadPool(String name) {
		return newCachedThreadPool(name, true);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newCachedThreadPool()}.
	 */
	public static ExecutorService newCachedThreadPool(String name, boolean daemon) {
		return Executors.newCachedThreadPool(new FactoryImpl(name, daemon));
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 *
	 * @return {@link Executors#newSingleThreadExecutor()}.
	 */
	public static ExecutorService newSingleThreadExecutor(String name) {
		return newSingleThreadExecutor(name, true);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newSingleThreadExecutor()}.
	 */
	public static ExecutorService newSingleThreadExecutor(String name, boolean daemon) {
		return Executors.newSingleThreadExecutor(new FactoryImpl(name, daemon));
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 *
	 * @return {@link Executors#newScheduledThreadPool(int)}.
	 */
	public static ScheduledExecutorService newScheduledThreadPool(String name) {
		return newScheduledThreadPool(name, true);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newScheduledThreadPool(int)}.
	 */
	public static ScheduledExecutorService newScheduledThreadPool(String name, boolean daemon) {
		return Executors.newScheduledThreadPool(MAX, new FactoryImpl(name, daemon));
	}

	private static class FactoryImpl implements ThreadFactory {
		private final String name;
		private final boolean daemon;
		private int tid = 0;

		public FactoryImpl(String name, boolean daemon) {
			this.name = name;
			this.daemon = daemon;
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(daemon);
			thread.setName(name + "-" + tid++);
			return thread;
		}
	}

}
