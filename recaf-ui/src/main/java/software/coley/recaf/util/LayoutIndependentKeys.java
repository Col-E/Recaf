package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.fxmisc.richtext.GenericStyledArea;

public final class LayoutIndependentKeys {
	private LayoutIndependentKeys() {
	}

	@Nonnull
	public static KeyCode resolveKeyCode(@Nonnull KeyEvent event) {
		KeyCode resolved = resolveFromControlCharacter(event);
		return resolved != null ? resolved : event.getCode();
	}

	public static boolean isModified(@Nonnull KeyEvent event, @Nonnull KeyCode code) {
		if (resolveKeyCode(event) != code)
			return false;
		return event.isControlDown() || event.isMetaDown();
	}

	public static void normalizeEvent(@Nonnull KeyEvent event) {
		if (event.isConsumed())
			return;
		KeyCode resolved = resolveFromControlCharacter(event);
		if (resolved == null || event.getCode() == resolved)
			return;
		if (!(event.getTarget() instanceof Node target) || !isTextInput(target))
			return;
		event.consume();
		Event.fireEvent(target, new KeyEvent(
				event.getEventType(),
				event.getCharacter(),
				event.getText(),
				resolved,
				event.isShiftDown(),
				event.isControlDown(),
				event.isAltDown(),
				event.isMetaDown()
		));
	}

	private static boolean isTextInput(@Nonnull Node target) {
		return target instanceof TextInputControl || target instanceof GenericStyledArea;
	}

	private static KeyCode resolveFromControlCharacter(@Nonnull KeyEvent event) {
		if (!event.isControlDown() && !event.isMetaDown())
			return null;
		String character = event.getCharacter();
		if (character.isEmpty())
			return null;
		char c = character.charAt(0);
		if (c < 1 || c > 26)
			return null;
		return NodeEvents.getKeycode((char) ('a' + c - 1));
	}
}
