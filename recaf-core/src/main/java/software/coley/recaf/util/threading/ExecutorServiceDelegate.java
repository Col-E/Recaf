package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Delegating impl of {@link ExecutorService} that also wraps tasks with {@link ThreadUtil#wrap(Runnable)}.
 *
 * @author Matt Coley
 */
public class ExecutorServiceDelegate implements ExecutorService {
	private final ExecutorService delegate;

	/**
	 * @param delegate
	 * 		Delegate service to pass to.
	 */
	public ExecutorServiceDelegate(@Nonnull ExecutorService delegate) {
		this.delegate = delegate;
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(@Nonnull Callable<T> task) {
		return delegate.submit(ThreadUtil.wrap(task));
	}

	@Override
	public <T> Future<T> submit(@Nonnull Runnable task, T result) {
		return delegate.submit(ThreadUtil.wrap(task), result);
	}

	@Override
	public Future<?> submit(@Nonnull Runnable task) {
		return delegate.submit(ThreadUtil.wrap(task));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return delegate.invokeAll(tasks.stream().map(ThreadUtil::wrap).toList());
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
		return delegate.invokeAll(tasks.stream().map(ThreadUtil::wrap).toList(), timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return delegate.invokeAny(tasks.stream().map(ThreadUtil::wrap).toList());
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.invokeAny(tasks.stream().map(ThreadUtil::wrap).toList(), timeout, unit);
	}

	@Override
	public void execute(@Nonnull Runnable command) {
		delegate.execute(ThreadUtil.wrap(command));
	}
}
