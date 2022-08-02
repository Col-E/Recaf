package me.coley.recaf.ui.behavior;

import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DisplayConfig;

import java.util.function.Consumer;

public interface FontSizeChangeable {

	void bindFontSize(IntegerProperty property);

	void applyEventsForFontSizeChange(Consumer<Node> consumer);

	Consumer<Node> DEFAULT_APPLIER = (node) -> {
		node.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (!e.isControlDown()) return;
			e.consume();
			advanceFontSize(e.getDeltaY() > 0);
		});
		node.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!e.isControlDown() || !(e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.SUBTRACT)) return;
			e.consume();
			advanceFontSize(e.getCode() == KeyCode.ADD);
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
