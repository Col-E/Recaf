package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static software.coley.recaf.util.threading.ThreadPoolFactory.newScheduledThreadPool;

/**
 * Common threading utility. Used for <i>"miscellaneous"</i> threads.
 * Larger thread operations should create their own pools using {@link ThreadPoolFactory}.
 *
 * @author Matt Coley
 */
public class ThreadUtil {
	private static final Logger logger = Logging.get(ThreadUtil.class);
	private static final ScheduledExecutorService scheduledService = newScheduledThreadPool("misc");

	/**
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Thread future.
	 */
	@Nonnull
	public static CompletableFuture<?> run(@Nonnull Runnable action) {
		return CompletableFuture.runAsync(wrap(action), scheduledService);
	}

	/**
	 * @param action
	 * 		Supplier to start in new thread.
	 *
	 * @return Thread future.
	 */
	@Nonnull
	public static <T> CompletableFuture<T> run(@Nonnull Supplier<T> action) {
		return CompletableFuture.supplyAsync(action, scheduledService);
	}

	/**
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	@Nonnull
	public static CompletableFuture<?> runDelayed(long delayMs, @Nonnull Runnable action) {
		CompletableFuture<?> future = new CompletableFuture<>();
		scheduledService.schedule(() -> {
			try {
				action.run();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		}, delayMs, TimeUnit.MILLISECONDS);
		return future;
	}

	/**
	 * Run a given action with a timeout.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param action
	 * 		Runnable to execute.
	 *
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int millis, @Nonnull Runnable action) {
		try {
			Future<?> future = run(action);
			return timeout(millis, future);
		} catch (Throwable t) {
			// Can be thrown by execution timeout
			return false;
		}
	}

	/**
	 * Give a thread future a time limit.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param future
	 * 		Thread future being run.
	 *
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int millis, @Nonnull Future<?> future) {
		try {
			future.get(millis, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException e) {
			// Expected: Timeout
			return false;
		} catch (Throwable t) {
			// Other error
			return true;
		}
	}


	/**
	 * Give a thread pool a time limit to finish all of its threads.
	 *
	 * @param millis
	 * 		Timeout in milliseconds.
	 * @param service
	 * 		Thread pool being used.
	 *
	 * @return {@code true} when thread pool completed before time.
	 * {@code false} when the thread pool did not finish, or was interrupted.
	 */
	public static boolean timeout(int millis, @Nonnull ExecutorService service) {
		try {
			// Shutdown so no new tasks are completed, but existing ones will finish.
			service.shutdown();
			// Wait until they finish. The prior shutdown request is required.
			// Calling 'awaitTermination' without calling shutdown will hang forever.
			return service.awaitTermination(millis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// A thread was interrupted so operation did not complete.
			return false;
		} catch (Throwable t) {
			// Other error
			return true;
		}
	}

	/**
	 * @param futures
	 * 		All futures to create a wrapper future of.
	 *
	 * @return Wrapper for all futures.
	 */
	@Nonnull
	public static CompletableFuture<Void> allOf(@Nonnull CompletableFuture<?>... futures) {
		AtomicBoolean thrown = new AtomicBoolean();
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
		for (CompletableFuture<?> f : futures) {
			f.exceptionally(t -> {
				if (thrown.compareAndSet(false, true)) {
					for (CompletableFuture<?> f1 : futures) {
						f1.completeExceptionally(t);
					}
					allOf.completeExceptionally(t);
				}
				return null;
			});
		}
		return allOf;
	}

	/**
	 * @param future
	 * 		Thread future being run.
	 *
	 * @return {@code true} on completion. {@code false} for interruption.
	 */
	public static boolean blockUntilComplete(@Nonnull Future<?> future) {
		return timeout(Integer.MAX_VALUE, future);
	}

	/**
	 * @param service
	 * 		Thread pool being used.
	 *
	 * @return {@code true} on completion. {@code false} for interruption.
	 */
	public static boolean blockUntilComplete(@Nonnull ExecutorService service) {
		return timeout(Integer.MAX_VALUE, service);
	}

	/**
	 * Submits a periodic action that becomes enabled first after the given initial delay,
	 * and subsequently with the given period.
	 *
	 * @param task
	 * 		Task to execute.
	 * @param initialDelay
	 * 		The time to delay first execution.
	 * @param period
	 * 		The period between successive executions.
	 * @param unit
	 * 		The time unit of the initialDelay
	 * 		and period parameters.
	 *
	 * @return future representing completion of the tasks.
	 *
	 * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	@Nonnull
	public static ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable task, long initialDelay,
	                                                     long period, @Nonnull TimeUnit unit) {
		return scheduledService.scheduleAtFixedRate(task, initialDelay, period, unit);
	}

	/**
	 * Wrap action to handle error logging.
	 * <br>
	 * <b>IMPORTANT:</b> This does <i>not work</i> with {@link ThreadFactory} for all thread-pool implementations.
	 * To support any {@link ExecutorService} type, instead wrap it with {@link ExecutorServiceDelegate} or
	 * {@link ScheduledExecutorServiceDelegate} which will delegate passed {@link Runnable} tasks here.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return Wrapper runnable.
	 */
	@Nonnull
	public static Runnable wrap(@Nonnull Runnable action) {
		return () -> {
			try {
				action.run();
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread '{}'", Thread.currentThread().getName(), t);
			}
		};
	}

	/**
	 * Wrap action to handle error logging.
	 * <br>
	 * <b>IMPORTANT:</b> This does <i>not work</i> with {@link ThreadFactory} for all thread-pool implementations.
	 * To support any {@link ExecutorService} type, instead wrap it with {@link ExecutorServiceDelegate} or
	 * {@link ScheduledExecutorServiceDelegate} which will delegate passed {@link Consumer} tasks here.
	 *
	 * @param action
	 * 		Consumer to run.
	 * @param <T>
	 * 		Consumer item type.
	 *
	 * @return Wrapper consumer.
	 */
	@Nonnull
	public static <T> Consumer<T> wrap(@Nonnull Consumer<T> action) {
		return in -> {
			try {
				action.accept(in);
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread '{}'", Thread.currentThread().getName(), t);
			}
		};
	}

	/**
	 * Wrap action to handle error logging.
	 * <br>
	 * <b>IMPORTANT:</b> This does <i>not work</i> with {@link ThreadFactory} for all thread-pool implementations.
	 * To support any {@link ExecutorService} type, instead wrap it with {@link ExecutorServiceDelegate} or
	 * {@link ScheduledExecutorServiceDelegate} which will delegate passed {@link Supplier} tasks here.
	 *
	 * @param action
	 * 		Supplier to provide a value.
	 * @param fallback
	 * 		Fallback value when the supplier action encounters an exception.
	 * @param <T>
	 * 		Supplier item type.
	 *
	 * @return Wrapper consumer.
	 */
	@Nonnull
	public static <T> Supplier<T> wrap(@Nonnull Supplier<T> action, @Nullable T fallback) {
		return () -> {
			try {
				return action.get();
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread '{}'", Thread.currentThread().getName(), t);
				return fallback;
			}
		};
	}

	/**
	 * Wrap action to handle error logging.
	 * <br>
	 * <b>IMPORTANT:</b> This does <i>not work</i> with {@link ThreadFactory} for all thread-pool implementations.
	 * To support any {@link ExecutorService} type, instead wrap it with {@link ExecutorServiceDelegate} or
	 * {@link ScheduledExecutorServiceDelegate} which will delegate passed {@link Callable} tasks here.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return Wrapper callable.
	 */
	@Nonnull
	public static <T> Callable<T> wrap(@Nonnull Callable<T> action) {
		return () -> {
			try {
				return action.call();
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread '{}'", Thread.currentThread().getName(), t);
				throw t;
			}
		};
	}

	/**
	 * Wraps our executor service into phasing executor that allows to wait for completion of all tasks passed into it.
	 *
	 * @return phasing executor service.
	 *
	 * @see PhasingExecutorService
	 */
	@Nonnull
	public static ExecutorService phasingService() {
		return phasingService(scheduledService);
	}

	/**
	 * Wraps our executor service into phasing executor that allows to wait for completion of all tasks passed into it.
	 *
	 * @param delegate
	 * 		Executor to delegate thread management to.
	 *
	 * @return phasing executor service.
	 *
	 * @see PhasingExecutorService
	 */
	@Nonnull
	public static ExecutorService phasingService(@Nonnull ExecutorService delegate) {
		return new PhasingExecutorService(delegate);
	}

	/**
	 * @return Backing executor.
	 */
	@Nonnull
	public static ScheduledExecutorService executor() {
		return scheduledService;
	}

	/**
	 * @param t
	 * 		Exception thrown.
	 * @param <V>
	 * 		Future type.
	 *
	 * @return Future of a failed execution due to a thrown error.
	 */
	@Nonnull
	public static <V> CompletableFuture<V> failedFuture(@Nonnull Throwable t) {
		CompletableFuture<V> future = new CompletableFuture<>();
		future.completeExceptionally(t);
		return future;
	}

	/**
	 * Shutdowns executors.
	 */
	public static void shutdown() {
		logger.trace("Shutting misc executors");
		scheduledService.shutdown();
	}
}