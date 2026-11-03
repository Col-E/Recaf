package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

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
