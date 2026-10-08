package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.List;

import static javafx.scene.input.KeyCode.*;

/**
 * An event handler to target {@link KeyEvent#KEY_TYPED} and {@link KeyEvent#KEY_RELEASED} which updates
 * the items and visibility state of a {@link CompletionPopup}.
 * <p>
 * Key-released events do not carry the typed character, so it is recorded from the key-typed event which precedes
 * the release. The release is handled after the editor has inserted the text.
 *
 * @author Matt Coley
 */
public class CompletionPopupUpdater<T> implements EventHandler<KeyEvent> {
	private final TabCompleter<T> tabCompleter;
	private final CompletionPopup<T> completionPopup;
	private String typedCharacter = "";

	public CompletionPopupUpdater(@Nonnull TabCompleter<T> tabCompleter,
	                              @Nonnull CompletionPopup<T> completionPopup) {
		this.tabCompleter = tabCompleter;
		this.completionPopup = completionPopup;
	}

	@Override
	public void handle(KeyEvent event) {
		if (event.getEventType() == KeyEvent.KEY_TYPED) {
			typedCharacter = event.getCharacter();
			return;
		}

		// Each release consumes the recorded character so it cannot apply to a later key release.
		String typed = typedCharacter;
		typedCharacter = "";
		KeyCode code = event.getCode();

		// Remove popup if backspace is pressed.
		if (code == BACK_SPACE) {
			// If the popup is shown, one backspace will be consumed, so we must
			// tell it to manually pass along a backspace to the editor.
			if (completionPopup.isShowing()) {
				completionPopup.handleBackspace();
				completionPopup.hide();
			}
			return;
		}

		// Remove popup if escape is pressed.
		if (code == ESCAPE) {
			completionPopup.hide();
			event.consume();
			return;
		}

		// Skip handling for navigation keys.
		// These move around the completion menu.
		if (code.isArrowKey() || code.isNavigationKey() || code == TAB || code == ENTER) {
			return;
		}

		// Any non-word character should remove the popup, unless the completer supports the typed character.
		// Modifiers are also excluded so capitalizing letters won't hide the popup.
		boolean specialCharacter = typed.length() == 1 && tabCompleter.isSpecialCompletableCharacter(typed.charAt(0));
		if (!code.isLetterKey() && !code.isDigitKey() && !code.isModifierKey() && code != PERIOD && !specialCharacter) {
			completionPopup.hide();
			return;
		}

		// Update the bounds, or hide the popup if the caret bounds cannot be found.
		if (!completionPopup.updateCaretBounds()) {
			completionPopup.hide();
			return;
		}

		// Hide if there is selected text. We only tab complete as a word is being written.
		if (completionPopup.hasTextSelection()) {
			completionPopup.hide();
			return;
		}

		// Fill completion candidates
		List<T> items = tabCompleter.computeCurrentCompletions();

		// Update the popup
		completionPopup.updateItems(items);
		if (!items.isEmpty())
			completionPopup.show();
		else
			completionPopup.hide();
	}
}
