package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.FileNavigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.pane.editing.binary.BinaryXmlFilePane;
import software.coley.recaf.ui.pane.editing.media.AudioFilePane;
import software.coley.recaf.ui.pane.editing.media.ImageFilePane;
import software.coley.recaf.ui.pane.editing.media.VideoFilePane;
import software.coley.recaf.ui.pane.editing.text.TextFilePane;

/**
 * Common outline for displaying {@link FileInfo} content.
 *
 * @author Matt Coley
 * @see TextFilePane
 * @see BinaryXmlFilePane
 * @see ImageFilePane
 * @see AudioFilePane
 * @see VideoFilePane
 */
public abstract class FilePane extends AbstractContentPane<FilePathNode> implements FileNavigable {
	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		// Update if class has changed.
		if (path instanceof FilePathNode filePath) {
			this.path = filePath;
			pathUpdateListeners.forEach(listener -> listener.accept(filePath));

			// Initialize UI if it has not been done yet.
			if (!hasDisplay())
				generateDisplay();

			// Notify children of change.
			getNavigableChildren().forEach(child -> {
				if (child instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(path);
			});
		}
	}

	@Nonnull
	@Override
	public FilePathNode getPath() {
		return path;
	}
}
