package me.coley.recaf.util.observable;

import com.sun.javafx.collections.ObservableSetWrapper;
import javafx.collections.SetChangeListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BatchObservableSetWrapper<E> extends ObservableSetWrapper<E> implements BatchObservableSet<E> {
	private final Set<Consumer<List<SetChangeListener.Change<? extends E>>>> setChangeListeners = new HashSet<>();
	private final AtomicLong latch;
	private final long amount;
	private final List<SetChangeListener.Change<? extends E>> cache = new ArrayList<>();

	/**
	 * Creates new instance of ObservableSet that wraps the particular set specified by the parameter set.
	 *
	 * @param set    the set being wrapped
	 * @param amount amount after which the batch listener should be triggered.
	 */
	public BatchObservableSetWrapper(Set<E> set, long amount) {
		super(set);
		this.latch = new AtomicLong(amount);
		this.amount = amount;
		addListener((SetChangeListener<E>) change -> {
			synchronized (cache) {
				cache.add(change);
				if (latch.decrementAndGet() <= 0)
					triggerBatchListeners(true);
			}
		});
	}

	@Override
	public void addBatchListener(Consumer<List<SetChangeListener.Change<? extends E>>> listener) {
		setChangeListeners.add(listener);
	}

	@Override
	public void removeBatchListener(Consumer<List<SetChangeListener.Change<? extends E>>> listener) {
		setChangeListeners.remove(listener);
	}

	@Override
	public void clear() {
		super.clear();
		triggerBatchListeners(true);
	}

	@Override
	public void triggerBatchListeners(boolean reset) {
		synchronized (cache) {
			var changes = List.copyOf(cache);
			if (reset) {
				latch.set(amount);
				cache.clear();
			}
			for (Consumer<List<SetChangeListener.Change<? extends E>>> listener : setChangeListeners) {
				listener.accept(changes);
			}
		}
	}
}
