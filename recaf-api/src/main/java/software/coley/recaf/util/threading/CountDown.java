package software.coley.recaf.util.threading;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * CountDown thread synchronizer.
 *
 * @author xDark
 */
public final class CountDown {
	private final Impl impl = new Impl();

	/**
	 * Waits for countdown to finish.
	 *
	 * @throws InterruptedException
	 * 		If thread got interrupted.
	 */
	public void await() throws InterruptedException {
		impl.acquireSharedInterruptibly(1);
	}

	/**
	 * Waits for countdown to finish.
	 *
	 * @param timeout
	 * 		The maximum period of time to wait.
	 * @param unit
	 * 		Time unit.
	 *
	 * @throws InterruptedException
	 * 		If thread got interrupted.
	 */
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return impl.tryAcquireSharedNanos(1, unit.toNanos(timeout));
	}

	/**
	 * Decrements the count by 1.
	 */
	public void release() {
		impl.releaseShared(1);
	}

	/**
	 * Increments the count by 1.
	 */
	public void register() {
		impl.bulkRegister(1);
	}

	/**
	 * Increments the count.
	 *
	 * @param count
	 * 		Number to increment the count by.
	 */
	public void bulkRegister(int count) {
		impl.bulkRegister(count);
	}

	private static final class Impl extends AbstractQueuedSynchronizer {

		void bulkRegister(int count) {
			while (true) {
				int oldCount = getState();
				int newCount = oldCount + count;
				if (compareAndSetState(oldCount, newCount)) {
					return;
				}
			}
		}

		@Override
		protected int tryAcquireShared(int acquires) {
			return (getState() == 0) ? 1 : -1;
		}

		@Override
		protected boolean tryReleaseShared(int releases) {
			while (true) {
				int state = getState();
				if (state == 0) {
					return false;
				}
				int newState = state - 1;
				if (compareAndSetState(state, newState)) {
					return newState == 0;
				}
			}
		}
	}
}
