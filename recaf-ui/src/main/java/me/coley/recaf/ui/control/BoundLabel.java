package me.coley.recaf.ui.control;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.Label;

/**
 * Utility label to quickly apply a {@link StringBinding}, commonly for UI translations.
 *
 * @author Matt Coley
 */
public class BoundLabel extends Label {
	/**
	 * @param binding
	 * 		Text binding.
	 */
	public BoundLabel(StringBinding binding) {
		textProperty().bind(binding);
	}
}
