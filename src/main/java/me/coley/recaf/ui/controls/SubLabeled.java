package me.coley.recaf.ui.controls;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Label with sub-label.
 *
 * @author Matt
 */
public class SubLabeled extends VBox {
	/**
	 * @param primary
	 * 		Main text, more prominent.
	 * @param secondary
	 * 		Secondary text, less prominent.
	 */
	public SubLabeled(String primary, String secondary) {
		Label lblPrimary = new Label(primary);
		Label lblSecondary = new Label(secondary);
		lblPrimary.getStyleClass().add("h1");
		lblSecondary.getStyleClass().add("faint");
		getChildren().addAll(lblPrimary, lblSecondary);
	}
}
