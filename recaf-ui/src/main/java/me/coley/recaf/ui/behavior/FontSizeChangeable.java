package me.coley.recaf.ui.behavior;

import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DisplayConfig;

import java.util.function.BiConsumer;

public interface FontSizeChangeable {

	void bindFontSize(IntegerProperty property);

	void applyEventsForFontSizeChange(BiConsumer<FontSizeChangeable, Node> consumer);

	BiConsumer<FontSizeChangeable, Node> DEFAULT_APPLIER = (fsc, node) -> {
		node.addEventFilter(ScrollEvent.SCROLL, e -> {
			if (!e.isControlDown()) return;
			e.consume();
			advanceFontSize(fsc, e.getDeltaY() > 0);
		});
		node.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!e.isControlDown() || !(e.getCode() == KeyCode.ADD || e.getCode() == KeyCode.SUBTRACT)) return;
			e.consume();
			advanceFontSize(fsc, e.getCode() == KeyCode.ADD);
		});
	};

	static void advanceFontSize(FontSizeChangeable fsc, boolean up) {
		int oldSize = Configs.display().fontSize.get();
		int newSize = clamp(
			DisplayConfig.FONT_SIZE_BOUND_LEFT,
			oldSize + (up ? 1 : -1),
			DisplayConfig.FONT_SIZE_BOUND_RIGHT);
		if (oldSize != newSize) {
			Configs.display().fontSize.set(newSize);
		}
	}

	private static int clamp(int min, int value, int max) {
		if (value < min) return min;
		return Math.min(value, max);
	}
}
