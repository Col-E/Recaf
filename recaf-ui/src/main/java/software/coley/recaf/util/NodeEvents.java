package software.coley.recaf.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import software.coley.collections.Unchecked;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * JavaFX node event handler utils.
 *
 * @author Matt Coley
 * @author xDark
 */
public class NodeEvents {
	private static final Int2ObjectMap<KeyCode> charToKeycode = new Int2ObjectArrayMap<>();

	static {
		for (KeyCode code : KeyCode.values()) {
			String charStr = code.getChar();
			if (!charStr.isEmpty()) charToKeycode.put(charStr.charAt(0), code);
		}
	}

	private NodeEvents() {
	}

	/**
	 * @param c
	 * 		Char to look up.
	 *
	 * @return Keycode for char. May be {@code null} for unsupported characters.
	 */
	@Nullable
	public static KeyCode getKeycode(char c) {
		return charToKeycode.get(c);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMousePressHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMousePressed;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMousePressed);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMouseClickHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMouseClicked;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMouseClicked);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMouseReleaseHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMouseReleased;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMouseReleased);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMouseEnterHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMouseEntered;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMouseEntered);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMouseExitHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMouseExited;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMouseExited);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addMouseMoveHandler(@Nonnull Node node, @Nonnull EventHandler<MouseEvent> handler) {
		Function<Node, EventHandler<? super MouseEvent>> original = Node::getOnMouseMoved;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnMouseMoved);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addKeyPressHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyPressed;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnKeyPressed);
	}

	/**
	 * @param node
	 * 		Node to remove from.
	 * @param handler
	 * 		Handler to remove.
	 */
	public static void removeKeyPressHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyPressed;
		removeHandler(node, handler, Unchecked.cast(original), Node::setOnKeyPressed);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addKeyReleaseHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyReleased;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnKeyReleased);
	}

	/**
	 * @param node
	 * 		Node to remove from.
	 * @param handler
	 * 		Handler to remove.
	 */
	public static void removeKeyReleaseHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyPressed;
		removeHandler(node, handler, Unchecked.cast(original), Node::setOnKeyPressed);
	}

	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addKeyTypedHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyTyped;
		addHandler(node, handler, Unchecked.cast(original), Node::setOnKeyTyped);
	}

	/**
	 * @param node
	 * 		Node to remove from.
	 * @param handler
	 * 		Handler to remove.
	 */
	public static void removeKeyTypedHandler(@Nonnull Node node, @Nonnull EventHandler<KeyEvent> handler) {
		Function<Node, EventHandler<? super KeyEvent>> original = Node::getOnKeyTyped;
		removeHandler(node, handler, Unchecked.cast(original), Node::setOnKeyTyped);
	}

	private static <T extends Event> void addHandler(@Nonnull Node node, @Nonnull EventHandler<T> handler,
	                                                 @Nonnull Function<Node, EventHandler<T>> handlerGetter,
	                                                 @Nonnull BiConsumer<Node, EventHandler<T>> handlerSetter) {
		EventHandler<T> oldHandler = handlerGetter.apply(node);
		handlerSetter.accept(node, new SplittingHandler<>(handler, oldHandler));
	}

	private static <T extends Event> void removeHandler(@Nonnull Node node, @Nonnull EventHandler<T> handler,
	                                                    @Nonnull Function<Node, EventHandler<T>> handlerGetter,
	                                                    @Nonnull BiConsumer<Node, EventHandler<T>> handlerSetter) {
		EventHandler<T> currentHandler = Unchecked.cast(handlerGetter.apply(node));
		if (currentHandler instanceof SplittingHandler<T> splittingHandler) {
			if (splittingHandler.primary == handler)
				handlerSetter.accept(node, splittingHandler.secondary);
			else
				splittingHandler.remove(handler);
		} else if (currentHandler == handler)
			handlerSetter.accept(node, null);
	}

	/**
	 * Runs an action on the given observable's value if it is present <i>(non-null)</i>,
	 * or once when it changes to another value.
	 *
	 * @param value
	 * 		Value to observe.
	 * @param action
	 * 		Action to run on the value.
	 * @param <T>
	 * 		Value type.
	 */
	public static <T> void runOnceIfPresentOrOnChange(@Nonnull ObservableValue<T> value, @Nonnull Consumer<T> action) {
		T current = value.getValue();
		if (current != null) action.accept(current);
		else runOnceOnChange(value, action);
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
	public static <T> void runOnceOnChange(@Nonnull ObservableValue<T> value, @Nonnull Consumer<T> action) {
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
	public static <T> void dispatchAndRemoveIf(@Nonnull ObservableValue<T> value, @Nonnull RemovalChangeListener<T> listener) {
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
	public static <T> void dispatchAndRemoveIf(@Nonnull ObservableValue<T> value, @Nonnull Predicate<? super T> test) {
		dispatchAndRemoveIf(value, (observable, oldValue, newValue) -> test.test(newValue));
	}

	/**
	 * Value change listener. Key difference from {@link ChangeListener} is the return value of
	 * {@link #changed(ObservableValue, Object, Object)}.
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
		 *
		 * @return {@code true} to remove the listener after this change.
		 * {@code false} to keep the listener after this change.
		 */
		boolean changed(ObservableValue<? extends T> observable, T oldValue, T newValue);
	}

	/**
	 * Splitting handler to simplify calls like {@link #addKeyPressHandler(Node, EventHandler)} and
	 * {@link #removeKeyPressHandler(Node, EventHandler)}.
	 *
	 * @author Matt Coley
	 */
	private static class SplittingHandler<T extends Event> implements EventHandler<T> {
		private final EventHandler<T> primary;
		private EventHandler<T> secondary;

		/**
		 * @param primary
		 * 		Handler to invoke.
		 * @param secondary
		 * 		Next in the chain to invoke.
		 */
		private SplittingHandler(@Nonnull EventHandler<T> primary,
		                         @Nullable EventHandler<T> secondary) {
			this.primary = primary;
			this.secondary = secondary;
		}

		@Override
		public void handle(T event) {
			if (secondary != null)
				secondary.handle(event);
			primary.handle(event);
		}

		/**
		 * @param handler
		 * 		Handler to remove from the wrapper chain.
		 */
		@SuppressWarnings({"rawtypes", "unchecked"})
		public void remove(EventHandler<T> handler) {
			if (secondary == handler)
				secondary = null;
			else if (secondary instanceof SplittingHandler nextWrapper)
				nextWrapper.remove(handler);
		}
	}
}