package software.coley.recaf.ui.wizard;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.pane.PathPromptPane;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wizard page to select a single path via {@link PathPromptPane}.
 *
 * @author Matt Coley
 */
public class SinglePathWizardPage extends Wizard.WizardPage {
	private final PathPromptPane pathPromptPane;

	/**
	 * @param title
	 * 		Title binding.
	 * @param recentFilesConfig
	 * 		Config to pull recent locations from, to populate initial directories of file/directory choosers.
	 */
	public SinglePathWizardPage(@Nonnull StringBinding title, @Nonnull RecentFilesConfig recentFilesConfig) {
		super(title);
		pathPromptPane = new PathPromptPane(recentFilesConfig);
	}

	@Override
	protected Node createDisplay() {
		// Map progression to existence of input path.
		canProgressProperty().bind(pathPromptPane.pathProperty().map(path -> path != null && Files.exists(path)));
		return pathPromptPane;
	}

	/**
	 * Not to be called until the Wizard is completed.
	 * This is to guarantee that the path is not {@code null}.
	 *
	 * @return Path value. While may be initially {@code null},
	 * it will always be present when the user progresses past this page.
	 */
	@Nonnull
	public Path getPath() {
		Path path = pathPromptPane.pathProperty().get();
		if (path == null) throw new IllegalStateException("Path has not yet been selected");
		return path;
	}
}
