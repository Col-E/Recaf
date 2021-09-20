package me.coley.recaf.ui.control;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A component that has a primary larger label, with a smaller label underneath it.
 *
 * @author Matt Coley
 */
public class SubLabeled extends VBox {
	private final String primary;
	private final String secondary;

	/**
	 * @param primary
	 * 		Main text, more prominent.
	 * @param secondary
	 * 		Secondary text, less prominent.
	 */
	public SubLabeled(String primary, String secondary) {
		this(primary, secondary, "h1");
	}

	/**
	 * @param primary
	 * 		Main text, more prominent.
	 * @param secondary
	 * 		Secondary text, less prominent.
	 * @param primaryClass
	 * 		Header style class.
	 */
	public SubLabeled(String primary, String secondary, String primaryClass) {
		this.primary = primary;
		this.secondary = secondary;
		Label lblPrimary = new Label(primary);
		Label lblSecondary = new Label(secondary);
		lblPrimary.getStyleClass().add(primaryClass);
		lblSecondary.getStyleClass().add("faint");
		getChildren().addAll(lblPrimary, lblSecondary);
	}

	/**
	 * @return Primary label text.
	 */
	public String getPrimaryText() {
		return primary;
	}

	/**
	 * @return Secondary label text.
	 */
	public String getSecondaryText() {
		return secondary;
	}
}
