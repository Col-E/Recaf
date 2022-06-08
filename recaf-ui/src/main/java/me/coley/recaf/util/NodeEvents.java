package me.coley.recaf.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.function.Predicate;

/**
 * JavaFX node event handler utils.
 *
 * @author Matt Coley
 * @author xDark
 */
public class NodeEvents {
	private NodeEvents() {
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addKeyPressHandler(Node node, EventHandler<? super KeyEvent> handler) {
		EventHandler<? super KeyEvent> oldHandler = node.getOnKeyPressed();
		node.setOnKeyPressed(e -> {
			if (oldHandler != null)
				oldHandler.handle(e);
			handler.handle(e);
		});
	}

	/**
	 * Registers an event listener and removes it,
	 * as soon as {@link RemovalChangeListener} returns {@code true}.
	 *
	 * @param value
	 * 		Value to register listener for.
	 * @param listener
	 * 		Listener to register.
	 * @param <T>
	 * 		Value type.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void dispatchAndRemoveIf(ObservableValue<T> value, RemovalChangeListener<? super T> listener) {
		ChangeListener<? super T>[] handle = new ChangeListener[1];
		handle[0] = (observable, oldValue, newValue) -> {
			if (listener.changed(observable, oldValue, newValue)) {
				FxThreadUtil.run(() -> observable.removeListener(handle[0]));
			}
		};
		value.addListener(handle[0]);
	}

	/**
	 * Registers an event listener and removes it,
	 * as soon as {@link RemovalChangeListener} returns {@code true}.
	 *
	 * @param value
	 * 		Value to register listener for.
	 * @param test
	 * 		Test function for a new value.
	 * @param <T>
	 * 		Value type.
	 */
	public static <T> void dispatchAndRemoveIf(ObservableValue<T> value, Predicate<? super T> test) {
		dispatchAndRemoveIf(value, (observable, oldValue, newValue) -> test.test(newValue));
	}

	/**
	 * Value change listener.
	 *
	 * @param <T>
	 * 		Value type.
	 *
	 * @author xDark
	 */
	public interface RemovalChangeListener<T> {
		/**
		 * Called when the value of an {@link ObservableValue} changes.
		 *
		 * @param observable
		 * 		The {@code ObservableValue} which value changed.
		 * @param oldValue
		 * 		The old value.
		 * @param newValue
		 * 		The new value.
		 */
		boolean changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
	}
}
