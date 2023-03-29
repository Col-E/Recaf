package me.coley.recaf.util.observable;

import javafx.beans.Observable;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface BatchObservableSet<E> extends ObservableSet<E> {
	void addBatchListener(Consumer<List<SetChangeListener.Change<? extends E>>> listener);
	void removeBatchListener(Consumer<List<SetChangeListener.Change<? extends E>>> listener);

	void triggerBatchListeners(boolean reset);

	default <OC extends Observable & Collection<E>> void batchListenAndApplyInto(OC observableCollection) {
		addBatchListener(changes -> {
			if (changes.isEmpty()) return;
			FxThreadUtil.dispatch(() -> {
				boolean justReset = false;
				for (SetChangeListener.Change<? extends E> change : changes) {
					if (change.wasAdded()) observableCollection.add(change.getElementAdded());
					else if (change.wasRemoved()) {
						observableCollection.remove(change.getElementRemoved());
					} else {
						justReset = true;
						break;
					}
				}
				if (justReset) {
					observableCollection.clear();
					observableCollection.addAll(this);
				}
			});
		});
	}
}
