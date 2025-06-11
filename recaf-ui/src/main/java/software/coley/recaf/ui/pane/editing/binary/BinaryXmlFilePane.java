package software.coley.recaf.ui.pane.editing.binary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.ui.pane.editing.FilePane;
import software.coley.recaf.ui.pane.editing.text.TextPane;

/**
 * Displays {@link BinaryXmlFileInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class BinaryXmlFilePane extends FilePane {
	private final Instance<DecodingXmlPane> textProvider;

	@Inject
	public BinaryXmlFilePane(@Nonnull Instance<DecodingXmlPane> textProvider) {
		this.textProvider = textProvider;
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (hasDisplay())
			return;

		// Update content in pane.
		setDisplay(textProvider.get());
	}
}
