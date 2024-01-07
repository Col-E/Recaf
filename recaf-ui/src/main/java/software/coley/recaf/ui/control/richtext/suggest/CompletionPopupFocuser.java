package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.UP;

/**
 * An event handler to target {@link KeyEvent#KEY_TYPED} which passes focus requests to the {@link CompletionPopup}.
 *
 * @author Matt Coley
 */
public class CompletionPopupFocuser implements EventHandler<KeyEvent> {
	private final CompletionPopup<?> completionPopup;

	/**
	 * @param completionPopup
	 * 		Popup to send focus events to.
	 */
	public CompletionPopupFocuser(@Nonnull CompletionPopup<?> completionPopup) {
		this.completionPopup = completionPopup;
	}

	@Override
	public void handle(@Nonnull KeyEvent event) {
		// Ensure directional / input keys are sent to the popup.
		KeyCode code = event.getCode();
		boolean move = code == UP || code == DOWN;
		if (completionPopup.isShowing() && move)
			completionPopup.requestFocus();
	}
}
