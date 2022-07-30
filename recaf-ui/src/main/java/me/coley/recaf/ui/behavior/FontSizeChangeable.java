package me.coley.recaf.ui.behavior;

import com.sun.javafx.util.Utils;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.DisplayConfig;

import java.util.function.BiConsumer;

public interface FontSizeChangeable {

	/**
	 * @param fontSize New font size.
	 */
	void setFontSize(int fontSize);

	void applyEventsForFontSizeChange(BiConsumer<FontSizeChangeable, Node> consumer);

	class FontSizeChangeListener implements ChangeListener<Number> {
		public final FontSizeChangeable fsc;

		public FontSizeChangeListener(FontSizeChangeable fsc) {
			this.fsc = fsc;
		}

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			fsc.setFontSize(newValue.intValue());
		}
	}

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
		int newSize = Utils.clamp(
			DisplayConfig.fontSizeBounds.getKey(),
			oldSize + (up ? 1 : -1),
			DisplayConfig.fontSizeBounds.getValue());
		if (oldSize != newSize) {
			Configs.display().fontSize.set(newSize);
		}
	}
}
