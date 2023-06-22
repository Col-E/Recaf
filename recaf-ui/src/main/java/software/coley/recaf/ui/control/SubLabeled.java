package software.coley.recaf.ui.control;

import atlantafx.base.theme.Styles;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A component that has a primary larger label, with a smaller label underneath it.
 *
 * @author Matt Coley
 */
public class SubLabeled extends VBox {
	private final ObservableValue<String> primary;
	private final ObservableValue<String> secondary;

	/**
	 * @param primary
	 * 		Main text, more prominent.
	 * @param secondary
	 * 		Secondary text, less prominent.
	 */
	public SubLabeled(ObservableValue<String> primary, ObservableValue<String> secondary) {
		this(primary, secondary, Styles.TITLE_3);
	}

	/**
	 * @param primary
	 * 		Main text, more prominent.
	 * @param secondary
	 * 		Secondary text, less prominent.
	 * @param primaryClass
	 * 		Header style class.
	 */
	public SubLabeled(ObservableValue<String> primary, ObservableValue<String> secondary, String primaryClass) {
		this.primary = primary;
		this.secondary = secondary;
		Label lblPrimary = new BoundLabel(primary);
		Label lblSecondary = new BoundLabel(secondary);
		lblPrimary.getStyleClass().add(primaryClass);
		lblSecondary.getStyleClass().add(Styles.TEXT_SUBTLE);
		getChildren().addAll(lblPrimary, lblSecondary);
	}

	/**
	 * @return Primary label text.
	 */
	public String getPrimaryText() {
		return primary.getValue();
	}

	/**
	 * @return Secondary label text.
	 */
	public String getSecondaryText() {
		return secondary.getValue();
	}
}
