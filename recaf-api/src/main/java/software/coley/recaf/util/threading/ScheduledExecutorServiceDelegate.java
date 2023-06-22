package software.coley.recaf.util.threading;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Delegating impl of {@link ScheduledExecutorService} that also wraps tasks with {@link ThreadUtil#wrap(Runnable)}.
 *
 * @author Matt Coley
 */
public class ScheduledExecutorServiceDelegate extends ExecutorServiceDelegate implements ScheduledExecutorService {
	private final ScheduledExecutorService delegate;

	/**
	 * @param delegate
	 * 		Delegate service to pass to.
	 */
	public ScheduledExecutorServiceDelegate(@Nonnull ScheduledExecutorService delegate) {
		super(delegate);
		this.delegate = delegate;
	}

	@Override
	public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
		return delegate.schedule(ThreadUtil.wrap(command), delay, unit);
	}

	@Override
	public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
		return delegate.schedule(ThreadUtil.wrap(callable), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
		return delegate.scheduleAtFixedRate(ThreadUtil.wrap(command), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
		return delegate.scheduleWithFixedDelay(ThreadUtil.wrap(command), initialDelay, delay, unit);
	}
}
