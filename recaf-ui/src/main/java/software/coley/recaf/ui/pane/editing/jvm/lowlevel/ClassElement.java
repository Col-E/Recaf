package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.input.ContextMenuEvent;
import software.coley.recaf.util.EscapeUtil;

/**
 * Outlines content displayable in a {@link JvmLowLevelPane}.
 *
 * @author Matt Coley
 */
public interface ClassElement {
	/**
	 * @return Prefix text.
	 */
	@Nonnull
	String prefix();

	/**
	 * @return Primary display text.
	 */
	@Nonnull
	String content();

	/**
	 * @return Graphic for the element.
	 */
	@Nullable
	Node graphic();

	/**
	 * @return Handler to provide a context menu for the element.
	 */
	@Nullable
	EventHandler<ContextMenuEvent> contextRequest();

	/**
	 * Configures a given tree cell with values from this element.
	 *
	 * @param cell
	 * 		Cell to configure.
	 */
	default void configureDisplay(@Nonnull TreeCell<ClassElement> cell) {
		Label prefixLabel = new Label(prefix() + ":", graphic());
		prefixLabel.getStyleClass().add(Styles.TEXT_BOLD);

		cell.setText(EscapeUtil.escapeStandardAndUnicodeWhitespace(content()));
		cell.setGraphic(prefixLabel);
		cell.setOnContextMenuRequested(contextRequest());
	}
}
