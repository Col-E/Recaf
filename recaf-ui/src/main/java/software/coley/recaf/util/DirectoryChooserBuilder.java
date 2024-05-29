package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import software.coley.observables.ObservableString;

import java.io.File;
import java.nio.file.Path;

/**
 * Builder for {@link DirectoryChooser} which filters out invalid inputs.
 *
 * @author Matt Coley
 */
public class DirectoryChooserBuilder {
	private String title;
	private File initialDirectory;

	/**
	 * @param title
	 * 		Dialog title.
	 *
	 * @return Self.
	 */
	@Nonnull
	public DirectoryChooserBuilder setTitle(@Nonnull String title) {
		this.title = title;
		return this;
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within and have selected.
	 *
	 * @return Self.
	 */
	@Nonnull
	public DirectoryChooserBuilder setInitialDirectory(@Nonnull ObservableString initialDirectory) {
		File file = initialDirectory.unboxingMap(File::new);
		return setInitialDirectory(file);
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within and have selected.
	 *
	 * @return Self.
	 */
	@Nonnull
	public DirectoryChooserBuilder setInitialDirectory(@Nonnull Path initialDirectory) {
		return setInitialDirectory(initialDirectory.toFile());
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within and have selected.
	 *
	 * @return Self.
	 */
	@Nonnull
	public DirectoryChooserBuilder setInitialDirectory(@Nonnull File initialDirectory) {
		this.initialDirectory = initialDirectory;
		return this;
	}

	/**
	 * @return New directory chooser from current settings.
	 */
	@Nonnull
	public DirectoryChooser build() {
		DirectoryChooser chooser = new DirectoryChooser();
		if (initialDirectory != null && initialDirectory.isDirectory())
			chooser.setInitialDirectory(initialDirectory);
		if (title != null)
			chooser.setTitle(title);
		return chooser;
	}

	/**
	 * @param owner
	 * 		The owner window of the displayed directory dialog.
	 *
	 * @return Selected directory path, otherwise {@code null} if cancelled.
	 */
	@Nullable
	public File pick(@Nullable Window owner) {
		return build().showDialog(owner);
	}
}
