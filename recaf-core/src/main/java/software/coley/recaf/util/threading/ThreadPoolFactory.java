package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

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
	private static final int MAX = Math.max(2, Runtime.getRuntime().availableProcessors() - 2);

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
		return new ExecutorServiceDelegate(Executors.newFixedThreadPool(Math.min(MAX, size), new FactoryImpl(name, daemon)));
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
		return new ExecutorServiceDelegate(Executors.newCachedThreadPool(new FactoryImpl(name, daemon)));
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
		return new ExecutorServiceDelegate(Executors.newSingleThreadExecutor(new FactoryImpl(name, daemon)));
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
		return newScheduledThreadPool(name, MAX, daemon);
	}

	/**
	 * @param name
	 * 		Thread pool name.
	 * @param size
	 * 		Thread pool size.
	 * @param daemon
	 * 		Flag to set created threads as daemon threads.
	 *
	 * @return {@link Executors#newScheduledThreadPool(int)}.
	 */
	public static ScheduledExecutorService newScheduledThreadPool(String name, int size, boolean daemon) {
		return new ScheduledExecutorServiceDelegate(Executors.newScheduledThreadPool(size, new FactoryImpl(name, daemon)));
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
		public Thread newThread(@Nonnull Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(daemon);
			thread.setName("Recaf-" + name + "-" + tid++);
			return thread;
		}
	}
}
