package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

import java.util.function.Consumer;
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
		node.setOnKeyPressed(new KeyPressWrapper(handler, oldHandler));
	}

	/**
	 * @param node
	 * 		Node to remove from.
	 * @param handler
	 * 		Handler to remove.
	 */
	public static void removeKeyPressHandler(Node node, EventHandler<? super KeyEvent> handler) {
		EventHandler<? super KeyEvent> currentHandler = node.getOnKeyPressed();
		if (currentHandler instanceof KeyPressWrapper keyPressWrapper) {
			if (keyPressWrapper.current == handler)
				node.setOnKeyPressed(keyPressWrapper.next);
			else
				keyPressWrapper.remove(handler);
		} else if (currentHandler == handler)
			node.setOnKeyPressed(null);
	}

	/**
	 * Runs an action on the given observable once when it changes.
	 *
	 * @param value
	 * 		Value to observe.
	 * @param action
	 * 		Action to run on new value.
	 * @param <T>
	 * 		Value type.
	 */
	public static <T> void runOnceOnChange(ObservableValue<T> value, Consumer<T> action) {
		value.addListener(new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends T> observableValue, T old, T current) {
				value.removeListener(this);
				action.accept(current);
			}
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

	/**
	 * Key-press handler to simplify {@link #addKeyPressHandler(Node, EventHandler)} and
	 * {@link #removeKeyPressHandler(Node, EventHandler)}.
	 *
	 * @author Matt Coley
	 */
	private static class KeyPressWrapper implements EventHandler<KeyEvent> {
		private final EventHandler<? super KeyEvent> current;
		private EventHandler<? super KeyEvent> next;

		/**
		 * @param current
		 * 		Handler to invoke.
		 * @param next
		 * 		Next in the chain to invoke.
		 */
		private KeyPressWrapper(@Nonnull EventHandler<? super KeyEvent> current,
								@Nullable EventHandler<? super KeyEvent> next) {
			this.current = current;
			this.next = next;
		}

		@Override
		public void handle(KeyEvent event) {
			if (next != null)
				next.handle(event);
			current.handle(event);
		}

		/**
		 * @param handler
		 * 		Handler to remove from the wrapper chain.
		 */
		public void remove(EventHandler<? super KeyEvent> handler) {
			if (next == handler)
				next = null;
			else if (next instanceof KeyPressWrapper nextWrapper)
				nextWrapper.remove(handler);
		}
	}
}