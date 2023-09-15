package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Executor service that uses {@link Phaser} to track
 * currently running tasks.
 * 
 * @author xDark
 */
public final class PhasingExecutorService implements ExecutorService {
	private final AtomicBoolean shutdown = new AtomicBoolean();
	private final Phaser phaser = new Phaser(1);
	private final ExecutorService delegate;

	/**
	 * @param delegate
	 * 		Backing executor service.
	 */
	public PhasingExecutorService(ExecutorService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void shutdown() {
		if (shutdown.compareAndSet(false, true)) {
			phaser.arriveAndDeregister();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isShutdown() {
		return phaser.isTerminated();
	}

	@Override
	public boolean isTerminated() {
		return phaser.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		try {
			return phaser.awaitAdvanceInterruptibly(0, timeout, unit) <= 0;
		} catch (TimeoutException e) {
			return false;
		}
	}

	@Nonnull
	@Override
	public <T> Future<T> submit(@Nonnull Callable<T> task) {
		phaser.register();
		return delegate.submit(wrap(task));
	}

	@Nonnull
	@Override
	public <T> Future<T> submit(@Nonnull Runnable task, T result) {
		phaser.register();
		return delegate.submit(wrap(task), result);
	}

	@Nonnull
	@Override
	public Future<?> submit(@Nonnull Runnable task) {
		phaser.register();
		return delegate.submit(wrap(task));
	}

	@Nonnull
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		phaser.bulkRegister(tasks.size());
		return delegate.invokeAll(tasks.stream().map(this::wrap).collect(Collectors.toList()));
	}

	@Nonnull
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		phaser.bulkRegister(tasks.size());
		return delegate.invokeAll(tasks.stream().map(this::wrap).collect(Collectors.toList()), timeout, unit);
	}

	@Override
	public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		phaser.register();
		delegate.execute(wrap(command));
	}
	
	private Runnable wrap(Runnable r) {
		return () -> {
			try {
				ThreadUtil.wrap(r).run();
			} finally {
				phaser.arriveAndDeregister();
			}
		};
	}
	
	private <T> Callable<T> wrap(Callable<T> c) {
		return () -> {
			try {
				return ThreadUtil.wrap(c).call();
			} finally {
				phaser.arriveAndDeregister();
			}
		};
	}
}
