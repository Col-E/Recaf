package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import software.coley.observables.ObservableString;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Builder for {@link FileChooser} which filters out invalid inputs.
 *
 * @author Matt Coley
 */
public class FileChooserBuilder {
	private String title;
	private String initialFileName;
	private String fileExtensionFilterName;
	private String[] fileExtensions;
	private File initialDirectory;

	/**
	 * @param title
	 * 		Dialog title.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setTitle(@Nonnull String title) {
		this.title = title;
		return this;
	}

	/**
	 * @param initialFileName
	 * 		Dialog's initial file name.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setInitialFileName(@Nonnull String initialFileName) {
		this.initialFileName = initialFileName;
		return this;
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setInitialDirectory(@Nonnull ObservableString initialDirectory) {
		File file = initialDirectory.unboxingMap(File::new);
		return setInitialDirectory(file);
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setInitialDirectory(@Nonnull Path initialDirectory) {
		return setInitialDirectory(initialDirectory.toFile());
	}

	/**
	 * @param initialDirectory
	 * 		Dialog's initial directory to open within.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setInitialDirectory(@Nonnull File initialDirectory) {
		this.initialDirectory = initialDirectory;
		return this;
	}

	/**
	 * @param fileExtensionFilterName
	 * 		Name of file extension filter.
	 * @param fileExtensions
	 * 		File extensions to show. Example: {@code "*.jar", "*.zip"}.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setFileExtensionFilter(@Nonnull String fileExtensionFilterName, @Nonnull List<String> fileExtensions) {
		return setFileExtensionFilterName(fileExtensionFilterName)
				.setFileExtensions(fileExtensions);
	}

	/**
	 * @param fileExtensionFilterName
	 * 		Name of file extension filter.
	 * @param fileExtensions
	 * 		File extensions to show. Example: {@code "*.jar", "*.zip"}.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setFileExtensionFilter(@Nonnull String fileExtensionFilterName, @Nonnull String... fileExtensions) {
		return setFileExtensionFilterName(fileExtensionFilterName)
				.setFileExtensions(fileExtensions);
	}

	/**
	 * @param fileExtensionFilterName
	 * 		Name of file extension filter.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setFileExtensionFilterName(@Nonnull String fileExtensionFilterName) {
		this.fileExtensionFilterName = fileExtensionFilterName;
		return this;
	}

	/**
	 * @param fileExtensions
	 * 		File extensions to show. Example: {@code "*.jar", "*.zip"}.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setFileExtensions(@Nonnull List<String> fileExtensions) {
		return setFileExtensions(fileExtensions.toArray(new String[0]));
	}

	/**
	 * @param fileExtensions
	 * 		File extensions to show. Example: {@code "*.jar", "*.zip"}.
	 *
	 * @return Self.
	 */
	@Nonnull
	public FileChooserBuilder setFileExtensions(@Nonnull String... fileExtensions) {
		this.fileExtensions = fileExtensions;
		return this;
	}

	/**
	 * @return New filer chooser from current settings.
	 */
	@Nonnull
	public FileChooser build() {
		FileChooser chooser = new FileChooser();
		if (initialFileName != null)
			chooser.setInitialFileName(initialFileName);
		if (fileExtensionFilterName != null && fileExtensions != null)
			chooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter(fileExtensionFilterName, fileExtensions));
		if (initialDirectory != null && initialDirectory.isDirectory())
			chooser.setInitialDirectory(initialDirectory);
		if (title != null)
			chooser.setTitle(title);
		return chooser;
	}

	/**
	 * @param owner
	 * 		The owner window of the displayed file dialog.
	 *
	 * @return Selected file path, otherwise {@code null} if cancelled.
	 */
	@Nullable
	public File save(@Nullable Window owner) {
		return build().showSaveDialog(owner);
	}

	/**
	 * @param owner
	 * 		The owner window of the displayed file dialog.
	 *
	 * @return Selected file path, otherwise {@code null} if cancelled.
	 */
	@Nullable
	public File open(@Nullable Window owner) {
		return build().showOpenDialog(owner);
	}

	/**
	 * @param owner
	 * 		The owner window of the displayed file dialog.
	 *
	 * @return Selected file paths, otherwise {@code null} if cancelled.
	 */
	@Nullable
	public List<File> openMultiple(@Nullable Window owner) {
		return build().showOpenMultipleDialog(owner);
	}
}
