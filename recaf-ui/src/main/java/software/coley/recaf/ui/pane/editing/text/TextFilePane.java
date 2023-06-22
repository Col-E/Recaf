package software.coley.recaf.ui.pane.editing.text;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.ui.pane.editing.FilePane;

/**
 * Displays {@link TextFileInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class TextFilePane extends FilePane {
	private final Instance<TextPane> textProvider;

	@Inject
	public TextFilePane(@Nonnull Instance<TextPane> textProvider) {
		this.textProvider = textProvider;
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (getCenter() != null)
			return;

		// Update content in pane.
		setDisplay(textProvider.get());
	}
}
