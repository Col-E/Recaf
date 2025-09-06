package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import software.coley.observables.ObservableString;
import software.coley.recaf.ui.config.RecentFilesConfig;

import java.io.File;
import java.util.function.Consumer;

/**
 * Bundle of file choosers. Each chooser will remember its last open directory <i>(unless cancelled)</i>.
 *
 * @author Matt Coley
 * @see FileChooserBuilder
 */
public class FileChooserBundle {
	private final FileChooser fileOpen;
	private final FileChooser fileExport;
	private final DirectoryChooser dirOpen;
	private final DirectoryChooser dirExport;
	private Consumer<File> fileOpenListener;
	private Consumer<File> fileExportListener;
	private Consumer<File> dirOpenListener;
	private Consumer<File> dirExportListener;

	/**
	 * @param fileOpen
	 * 		File open chooser.
	 * @param fileExport
	 * 		File export chooser.
	 * @param dirOpen
	 * 		Directory open chooser.
	 * @param dirExport
	 * 		Directory export chooser.
	 */
	public FileChooserBundle(@Nonnull FileChooser fileOpen,
	                         @Nonnull FileChooser fileExport,
	                         @Nonnull DirectoryChooser dirOpen,
	                         @Nonnull DirectoryChooser dirExport) {
		this.fileOpen = fileOpen;
		this.fileExport = fileExport;
		this.dirOpen = dirOpen;
		this.dirExport = dirExport;
	}

	/**
	 * @param recentFiles
	 * 		Config to pull recent directories from.
	 *
	 * @return Bundle of choosers that initially open from recently interacted with directories.
	 */
	@Nonnull
	public static FileChooserBundle fromRecent(@Nonnull RecentFilesConfig recentFiles) {
		// Use a shared file-chooser for mapping menu actions.
		// That way there is some continuity when working with mappings.
		ObservableString lastOpenDirectory = recentFiles.getLastWorkspaceOpenDirectory();
		FileChooser fileOpen = new FileChooserBuilder()
				.setInitialDirectory(lastOpenDirectory)
				.setTitle(Lang.get("dialog.file.open"))
				.build();
		FileChooser fileExport = new FileChooserBuilder()
				.setInitialDirectory(lastOpenDirectory)
				.setTitle(Lang.get("dialog.file.save"))
				.build();
		DirectoryChooser dirOpen = new DirectoryChooserBuilder()
				.setInitialDirectory(lastOpenDirectory)
				.setTitle(Lang.get("dialog.file.open"))
				.build();
		DirectoryChooser dirExport = new DirectoryChooserBuilder()
				.setInitialDirectory(lastOpenDirectory)
				.setTitle(Lang.get("dialog.file.save"))
				.build();
		FileChooserBundle bundle = new FileChooserBundle(fileOpen, fileExport, dirOpen, dirExport);
		Consumer<File> fileConsumer = file -> {
			if (file != null && !file.isDirectory())
				file = file.getParentFile();
			if (file == null)
				return;
			lastOpenDirectory.setValue(file.getAbsolutePath());
		};
		bundle.fileOpenListener = fileConsumer;
		bundle.fileExportListener = fileConsumer;
		bundle.dirOpenListener = fileConsumer;
		bundle.dirExportListener = fileConsumer;
		return bundle;
	}

	/**
	 * @param window
	 * 		Window to re-focus when completed.
	 *
	 * @return Selected file, {@code null} when cancelled.
	 */
	@Nullable
	public File showFileOpen(@Nullable Window window) {
		File file = fileOpen.showOpenDialog(window);
		if (file != null)
			fileOpen.setInitialDirectory(file.getParentFile());
		if (fileOpenListener != null)
			fileOpenListener.accept(file);
		return file;
	}

	/**
	 * @param window
	 * 		Window to re-focus when completed.
	 *
	 * @return Selected file, {@code null} when cancelled.
	 */
	@Nullable
	public File showFileExport(@Nullable Window window) {
		File file = fileExport.showSaveDialog(window);
		if (file != null)
			fileExport.setInitialDirectory(file.getParentFile());
		if (fileExportListener != null)
			fileExportListener.accept(file);
		return file;
	}

	/**
	 * @param window
	 * 		Window to re-focus when completed.
	 *
	 * @return Selected directory, {@code null} when cancelled.
	 */
	@Nullable
	public File showDirOpen(@Nullable Window window) {
		File file = dirOpen.showDialog(window);
		if (file != null)
			dirOpen.setInitialDirectory(file.getParentFile());
		if (dirOpenListener != null)
			dirOpenListener.accept(file);
		return file;
	}

	/**
	 * @param window
	 * 		Window to re-focus when completed.
	 *
	 * @return Selected directory, {@code null} when cancelled.
	 */
	@Nullable
	public File showDirExport(@Nullable Window window) {
		File file = dirExport.showDialog(window);
		if (file != null)
			dirExport.setInitialDirectory(file.getParentFile());
		if (dirExportListener != null)
			dirExportListener.accept(file);
		return file;
	}

	/**
	 * @return File open chooser.
	 */
	@Nonnull
	public FileChooser getFileOpen() {
		return fileOpen;
	}

	/**
	 * @return File export chooser.
	 */
	@Nonnull
	public FileChooser getFileExport() {
		return fileExport;
	}

	/**
	 * @return Directory open chooser.
	 */
	@Nonnull
	public DirectoryChooser getDirOpen() {
		return dirOpen;
	}

	/**
	 * @return Directory export chooser.
	 */
	@Nonnull
	public DirectoryChooser getDirExport() {
		return dirExport;
	}

	/**
	 * @param fileOpenListener
	 * 		Listener to intercept items from {@link #getFileOpen()}.
	 */
	public void setFileOpenListener(@Nullable Consumer<File> fileOpenListener) {
		this.fileOpenListener = fileOpenListener;
	}

	/**
	 * @param fileExportListener
	 * 		Listener to intercept items from {@link #getFileExport()}.
	 */
	public void setFileExportListener(@Nullable Consumer<File> fileExportListener) {
		this.fileExportListener = fileExportListener;
	}

	/**
	 * @param dirOpenListener
	 * 		Listener to intercept items from {@link #getDirOpen()}.
	 */
	public void setDirOpenListener(@Nullable Consumer<File> dirOpenListener) {
		this.dirOpenListener = dirOpenListener;
	}

	/**
	 * @param dirExportListener
	 * 		Listener to intercept items from {@link #getDirExport()}.
	 */
	public void setDirExportListener(@Nullable Consumer<File> dirExportListener) {
		this.dirExportListener = dirExportListener;
	}
}
