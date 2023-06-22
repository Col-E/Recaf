package software.coley.recaf.util;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ChangeListener;

/**
 * Synchronized implementation of {@link StringBinding}.
 *
 * @author xDark
 */
public abstract class SynchronizedStringBinding extends StringBinding {
	@Override
	public synchronized String getValue() {
		return super.getValue();
	}

	@Override
	protected synchronized void onInvalidating() {
		super.onInvalidating();
	}

	@Override
	public synchronized void addListener(ChangeListener<? super String> listener) {
		super.addListener(listener);
	}

	@Override
	public synchronized void addListener(InvalidationListener listener) {
		super.addListener(listener);
	}

	@Override
	public synchronized void removeListener(ChangeListener<? super String> listener) {
		super.removeListener(listener);
	}

	@Override
	public synchronized void removeListener(InvalidationListener listener) {
		super.removeListener(listener);
	}
}