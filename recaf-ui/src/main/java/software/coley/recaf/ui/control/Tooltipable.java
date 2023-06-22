package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import software.coley.recaf.util.Lang;

/**
 * Support for tooltips with bindings to {@link Lang} for any supported control type.
 *
 * @author Matt Coley
 */
public interface Tooltipable {
	/**
	 * Implemented by {@link Control#setTooltip(Tooltip)}.
	 *
	 * @param tooltip
	 * 		Tooltip to assign.
	 */
	void setTooltip(Tooltip tooltip);

	/**
	 * @param tooltipKey
	 * 		Translation key for tooltip display.
	 *
	 * @return Self.
	 */
	@Nonnull
	default <T extends Tooltipable> T withTooltip(@Nonnull String tooltipKey) {
		return withTooltip(Lang.getBinding(tooltipKey));
	}

	/**
	 * @param tooltipValue
	 * 		Text binding value for tooltip display.
	 *
	 * @return Self.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	default <T extends Tooltipable> T withTooltip(@Nonnull ObservableValue<String> tooltipValue) {
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(tooltipValue);
		setTooltip(tooltip);
		return (T) this;
	}
}
