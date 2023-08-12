package software.coley.recaf.ui.pane.editing.image;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.ImageFileInfo;
import software.coley.recaf.ui.pane.editing.FilePane;

/**
 * Displays {@link ImageFileInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class ImageFilePane extends FilePane {
	private final Instance<ImagePane> imageProvider;

	@Inject
	public ImageFilePane(@Nonnull Instance<ImagePane> imageProvider) {
		this.imageProvider = imageProvider;
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (getCenter() != null)
			return;

		// Update content in pane.
		setDisplay(imageProvider.get());
	}
}
