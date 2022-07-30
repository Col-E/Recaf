package me.coley.recaf.util.threading;

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
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		try {
			return phaser.awaitAdvanceInterruptibly(0, timeout, unit) <= 0;
		} catch (TimeoutException e) {
			return false;
		}
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		phaser.register();
		return delegate.submit(wrap(task));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		phaser.register();
		return delegate.submit(wrap(task), result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		phaser.register();
		return delegate.submit(wrap(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		phaser.bulkRegister(tasks.size());
		return delegate.invokeAll(tasks.stream().map(this::wrap).collect(Collectors.toList()));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		phaser.bulkRegister(tasks.size());
		return delegate.invokeAll(tasks.stream().map(this::wrap).collect(Collectors.toList()), timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void execute(Runnable command) {
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
