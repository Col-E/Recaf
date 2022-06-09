package me.coley.recaf.ui.util;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.Label;
import me.coley.recaf.ui.control.BoundLabel;

import java.util.Objects;

/**
 * Utility for creating formatted {@link BoundLabel}s.
 *
 * @author Matt Coley
 */
public class Labels {
	/**
	 * @param langBinding
	 * 		Language binding for label display.
	 *
	 * @return Label bound to translatable text.
	 */
	public static Label make(StringBinding langBinding) {
		Objects.requireNonNull(langBinding, "Language binding cannot be null!");
		return new BoundLabel(langBinding);
	}

	/**
	 * Used to display bullet point format.
	 *
	 * @param langBinding
	 * 		Language binding for label display.
	 * @param secondaryText
	 * 		Text to appear after the initial binding text.
	 *
	 * @return Label bound to translatable text.
	 */
	public static Label makeAttribLabel(StringBinding langBinding, String secondaryText) {
		Label label = new Label(secondaryText);
		if (langBinding != null) {
			label.textProperty().bind(new StringBinding() {
				{
					bind(langBinding);
				}

				@Override
				protected String computeValue() {
					return String.format("  • %s: %s", langBinding.get(), secondaryText);
				}
			});
		}
		return label;
	}
}
