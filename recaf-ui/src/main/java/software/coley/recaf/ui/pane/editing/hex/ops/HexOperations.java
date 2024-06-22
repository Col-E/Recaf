package software.coley.recaf.ui.pane.editing.hex.ops;

import jakarta.annotation.Nonnull;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Outline holding the data model and UI operations for the hex editor.
 *
 * @author Matt Coley
 */
public interface HexOperations {
	@Nonnull
	HexAccess currentAccess();

	@Nonnull
	HexAccess originalAccess();

	@Nonnull
	HexNavigation navigation();

	void refreshDisplay(int offset, boolean asciiOrigin);

	void engageCurrent();

	void cancelCurrent();

	void sendKeyToCurrentEngaged(@Nonnull KeyCode code);

	@Nonnull
	EventHandler<KeyEvent> keyListener();
}
