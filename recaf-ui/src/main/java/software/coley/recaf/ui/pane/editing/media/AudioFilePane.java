package software.coley.recaf.ui.pane.editing.media;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.info.AudioFileInfo;
import software.coley.recaf.ui.pane.editing.FilePane;

/**
 * File pane for {@link AudioFileInfo}.
 *
 * @author Matt Coley
 */
@Dependent
public class AudioFilePane extends FilePane {
	private final Instance<AudioPane> audioProvider;

	@Inject
	public AudioFilePane(@Nonnull Instance<AudioPane> audioProvider) {
		this.audioProvider = audioProvider;
	}

	@Override
	protected void generateDisplay() {
		// If you want to swap out the display, first clear the existing one.
		// Clearing is done automatically when changing the editor type.
		if (hasDisplay())
			return;

		// Update content in pane.
		setDisplay(audioProvider.get());
	}
}
