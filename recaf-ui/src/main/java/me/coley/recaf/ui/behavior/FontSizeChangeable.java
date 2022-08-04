package me.coley.recaf.ui.behavior;

import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DisplayConfig;
import me.coley.recaf.config.container.KeybindConfig;

import java.util.function.Consumer;

/**
 * Behaviour to be able to change the font size by ctrl+scrolling and keybindings.
 * <p>
 * Implementation notes: <p>
 * To implement this behaviour, the following steps are required: <br>
 * Firstly the interface should be implemented on all levels of the chain,
 * from the highest - which invokes {@link #applyEventsForFontSizeChange(Consumer)}
 * after creation of a lower level (note: {@link me.coley.recaf.ui.docking.RecafDockingManager} is hooked upon tab creation,
 * so the tab needs to also implement it if a pane of it needs this behaviour) -
 * to the lowest which calls {@code consumer.accept(this) } in {@link #applyEventsForFontSizeChange(Consumer)}
 * for hooking the scroll and key events. <p>
 * Secondly, {@link #bindFontSize(IntegerProperty)} should be passed down until it reaches the lowest level
 * where the font size is either bound to the style or a {@link javafx.beans.value.ChangeListener} is used for changing the style.
 * <p>
 * Note: {@link javafx.beans.value.WeakChangeListener} should be used when dealing with panes which are being thrown away after use.
 * How? Add a field with the {@link javafx.beans.value.ChangeListener},
 * then pass it to {@link javafx.beans.value.ObservableValue#addListener(ChangeListener)}
 * with {@link javafx.beans.value.WeakChangeListener#WeakChangeListener(ChangeListener)}.
 *
 * @author Amejonah
 */
public interface FontSizeChangeable {
	/**
	 * Bind the font size to the style. In the chain,<br>
	 * if it's the highest level: {@code Configs.display().fontSize} can be passed down<br>
	 * if it's a middle level: the underlying level should be invoked with this method and pass property down<br>
	 * if it's the lowest level: the property should be bound to the style<br>
	 *
	 * @param property
	 * 		the font size property to bind to the style.
	 */
	void bindFontSize(IntegerProperty property);

	/**
	 * Apply the events for font size change. In the chain,<br>
	 * if it's the highest level: the underlying level should be invoked with this method and {@link #DEFAULT_APPLIER}<br>
	 * if it's a middle level: the underlying level should be invoked with this method and pass consumer down<br>
	 * if it's the lowest level: {@code consumer.accept(this) }<br>
	 *
	 * @param consumer
	 * 		the consumer to invoke on the lowest level for hooking scroll and keybinding event.
	 */
	void applyEventsForFontSizeChange(Consumer<Node> consumer);

	/**
	 * Default implementation of the Consumer for {@link #applyEventsForFontSizeChange(Consumer)} to use.
	 * Takes the node as parameter for which the scroll and keybindings should be applied.
	 */
	Consumer<Node> DEFAULT_APPLIER = (node) -> {
		node.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (!e.isControlDown()) return;
			e.consume();
			advanceFontSize(e.getDeltaY() > 0);
		});
		node.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			KeybindConfig keybinds = Configs.keybinds();
			if (!keybinds.fontSizeUp.match(e) && !keybinds.fontSizeDown.match(e)) return;
			e.consume();
			advanceFontSize(keybinds.fontSizeUp.match(e));
		});
	};

	static void advanceFontSize(boolean up) {
		int oldSize = Configs.display().fontSize.get();
		int newSize = clamp(oldSize + (up ? 1 : -1));
		if (oldSize != newSize) {
			Configs.display().fontSize.set(newSize);
		}
	}

	private static int clamp(int value) {
		if (value < DisplayConfig.FONT_SIZE_BOUND_LEFT) return DisplayConfig.FONT_SIZE_BOUND_LEFT;
		return Math.min(value, DisplayConfig.FONT_SIZE_BOUND_RIGHT);
	}
}
