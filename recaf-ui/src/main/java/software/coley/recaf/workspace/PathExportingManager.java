package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import software.coley.observables.ObservableString;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.InputFilePathProperty;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.io.PathWorkspaceExportConsumer;
import software.coley.recaf.services.workspace.io.WorkspaceCompressType;
import software.coley.recaf.services.workspace.io.WorkspaceExportOptions;
import software.coley.recaf.services.workspace.io.WorkspaceExporter;
import software.coley.recaf.services.workspace.io.WorkspaceOutputType;
import software.coley.recaf.ui.config.ExportConfig;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.util.DirectoryChooserBuilder;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FileChooserBuilder;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.io.IOUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Manager module handle exporting {@link Info} and {@link Workspace} instances to {@link Path}s.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PathExportingManager {
	private static final Logger logger = Logging.get(PathExportingManager.class);
	private final Map<Workspace, Path> sessionExportPaths = Collections.synchronizedMap(new IdentityHashMap<>());
	private final WorkspaceManager workspaceManager;
	private final ExportConfig exportConfig;
	private final RecentFilesConfig recentFilesConfig;

	@Inject
	public PathExportingManager(WorkspaceManager workspaceManager,
	                            ExportConfig exportConfig,
	                            RecentFilesConfig recentFilesConfig) {
		this.workspaceManager = workspaceManager;
		this.exportConfig = exportConfig;
		this.recentFilesConfig = recentFilesConfig;

		// We don't want to keep workspaces in-memory, for obvious reasons...
		workspaceManager.addWorkspaceCloseListener(sessionExportPaths::remove);
	}

	/**
	 * Export the current workspace.
	 */
	public void exportCurrent() {
		// Validate current workspace.
		if (!workspaceManager.hasCurrentWorkspace())
			throw new IllegalStateException("Tried to export when no workspace was active!");

		// And export it.
		export(workspaceManager.getCurrent());
	}

	/**
	 * Export the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 */
	public void export(@Nonnull Workspace workspace) {
		export(workspace, "workspace", true);
	}

	/**
	 * Export the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 * @param contentDescription
	 * 		Description string for what is within the workspace.
	 * @param alertWhenEmpty
	 *        {@code true} to warn users before exporting that no changes are present in the workspace.
	 *        {@code false} to skip the warning.
	 */
	public void export(@Nonnull Workspace workspace, @Nonnull String contentDescription, boolean alertWhenEmpty) {
		WorkspaceResource primaryResource = workspace.getPrimaryResource();

		// Check if the user hasn't made any changes. Plenty of people have not understood that their changes weren't
		// saved for one reason or another (the amount of people seeing a red flash thinking that is fine is crazy)
		if (alertWhenEmpty) {
			boolean noChangesFound = exportConfig.getWarnNoChanges().getValue() && primaryResource.bundleStreamRecursive()
					.allMatch(b -> b.getDirtyKeys().isEmpty());
			if (noChangesFound) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION, Lang.get("dialog.file.nochanges"), ButtonType.YES, ButtonType.NO);
				alert.setTitle(Lang.get("dialog.title.nochanges"));
				Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
				stage.getIcons().add(Icons.getImage(Icons.LOGO));
				if (alert.showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
					return;
			}
		}

		// Prompt for a path to export to. If the user cancels, then we don't export.
		Path exportPath = chooseWorkspacePath(workspace);
		if (exportPath == null)
			return;

		try {
			exportToPath(workspace, exportPath, false);
			logger.info("Exported {} to path '{}'", contentDescription, exportPath);
		} catch (IOException ex) {
			logger.error("Failed to export {} to path '{}'", contentDescription, exportPath, ex);
			showWorkspaceExportError(ex);
		}
	}

	/**
	 * Export a workspace to a complete destination path.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 * @param path
	 * 		Complete destination path.
	 * @param automatic
	 *        {@code true} to stage and safely install the output.
	 *
	 * @throws IOException
	 * 		When export generation or installation fails.
	 */
	public void exportToPath(@Nonnull Workspace workspace, @Nonnull Path path, boolean automatic) throws IOException {
		WorkspaceResource primaryResource = workspace.getPrimaryResource();

		// Determine the export mode. Automatic exports should be staged to avoid overwriting the destination if the export fails.
		// But for manual exports, we can write directly to the destination since the user is (hopefully) aware of the risk.
		PathWorkspaceExportConsumer.Mode mode = PathWorkspaceExportConsumer.Mode.DIRECT;
		if (automatic) {
			mode = primaryResource instanceof WorkspaceDirectoryResource
					? PathWorkspaceExportConsumer.Mode.MIRROR_DIRECTORY
					: PathWorkspaceExportConsumer.Mode.ATOMIC_FILE;
		}

		// Export the workspace to the given path.
		try (PathWorkspaceExportConsumer consumer = new PathWorkspaceExportConsumer(path, mode)) {
			createResourceExporter(primaryResource, consumer).export(workspace);
		}

		// Exports of temporary workspace copies must not change the active workspace's destination.
		if (workspaceManager.hasCurrentWorkspace() && workspace == workspaceManager.getCurrent()) {
			if (RecentFilesConfig.ResourceModel.isSupported(primaryResource))
				recentFilesConfig.recordWorkspaceExportPath(workspace, path);
			else
				sessionExportPaths.put(workspace, path.toAbsolutePath().normalize());
		}
	}

	/**
	 * Export a workspace to its latest successful destination, source path, or a user-selected fallback.
	 * Intended to be used for {@link ExportConfig#getAutoExportAfterSave() automatic exports} after a
	 * workspace save operation.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 *
	 * @return {@code true} when an export completed, or {@code false} when the fallback chooser was canceled.
	 *
	 * @throws IOException
	 * 		When export generation or installation fails.
	 * @see WorkspaceSaveService
	 */
	public boolean exportAutomatically(@Nonnull Workspace workspace) throws IOException {
		Path destination = resolveAutomaticDestination(workspace);
		if (destination == null) {
			destination = FxThreadUtil.supply(() -> chooseWorkspacePath(workspace), null).join();
			if (destination == null)
				return false;
		}

		exportToPath(workspace, destination, true);
		return true;
	}

	/**
	 * Error reporting after a failure in {@link #exportAutomatically(Workspace)}.
	 *
	 * @param path
	 * 		Failed destination.
	 * @param failure
	 * 		Export failure.
	 *
	 * @see WorkspaceSaveService
	 */
	public void reportWorkspaceExportFailure(@Nullable Path path, @Nonnull IOException failure) {
		if (path == null)
			logger.error("Failed to automatically export workspace", failure);
		else
			logger.error("Failed to export workspace to path '{}'", path, failure);
		showWorkspaceExportError(failure);
	}

	/**
	 * Resolve the destination path for an automatic export.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 *
	 * @return Destination path, or {@code null} if no usable path was found.
	 */
	private @Nullable Path resolveAutomaticDestination(@Nonnull Workspace workspace) {
		// Check if the user has a preferred path for this workspace.
		Path remembered = recentFilesConfig.getWorkspaceExportPath(workspace);
		if (remembered == null) {
			synchronized (sessionExportPaths) {
				remembered = sessionExportPaths.get(workspace);
			}
		}

		// That preferred path may not be usable, so check if it is still valid.
		if (isUsableAutomaticTarget(workspace, remembered))
			return remembered.toAbsolutePath().normalize();

		// It wasn't valid... so next check if the workspace has a source path that is usable.
		WorkspaceResource primary = workspace.getPrimaryResource();
		Path original = null;
		if (primary instanceof WorkspaceDirectoryResource directoryResource) {
			original = directoryResource.getDirectoryPath();
		} else if (primary instanceof WorkspaceFileResource fileResource) {
			original = InputFilePathProperty.get(fileResource.getFileInfo());
		}
		if (isUsableAutomaticTarget(workspace, original))
			return original.toAbsolutePath().normalize();

		// No usable path was found, so the user will need to select one.
		return null;
	}

	/**
	 * Check if the given path is usable for an automatic export.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 * @param candidate
	 * 		Candidate path to check.
	 *
	 * @return {@code true} when the path is usable, otherwise {@code false}.
	 */
	private static boolean isUsableAutomaticTarget(@Nonnull Workspace workspace, @Nullable Path candidate) {
		if (candidate == null)
			return false;

		// The containing directory should exist and be writable.
		Path target = candidate.toAbsolutePath().normalize();
		Path parent = target.getParent();
		if (parent == null || !Files.isDirectory(parent) || !Files.isWritable(parent))
			return false;

		// If the target path doesn't exist then we can write to it.
		if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS))
			return true;

		// If the target path does exist, then it must be the same type of resource as the workspace's primary resource.
		if (workspace.getPrimaryResource() instanceof WorkspaceDirectoryResource)
			return Files.isDirectory(target);
		return Files.isRegularFile(target);
	}

	/**
	 * Prompt the user for a path to export the workspace to.
	 *
	 * @param workspace
	 * 		Workspace to export.
	 *
	 * @return Selected path, or {@code null} if the user canceled the prompt.
	 */
	@Nullable
	private Path chooseWorkspacePath(@Nonnull Workspace workspace) {
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		ObservableString lastWorkspaceExportDir = recentFilesConfig.getLastWorkspaceExportDirectory();
		File lastExportDir = lastWorkspaceExportDir.unboxingMap(File::new);
		File selectedPath;
		if (primaryResource instanceof WorkspaceDirectoryResource) {
			DirectoryChooser chooser = new DirectoryChooserBuilder()
					.setInitialDirectory(lastExportDir)
					.setTitle(Lang.get("dialog.file.export"))
					.build();
			selectedPath = chooser.showDialog(null);
		} else {
			FileChooserBuilder builder = new FileChooserBuilder()
					.setInitialDirectory(lastExportDir)
					.setTitle(Lang.get("dialog.file.export"));

			if (primaryResource instanceof WorkspaceFileResource primaryFileResource) {
				FileInfo backingFile = primaryFileResource.getFileInfo();

				// Use original file name as initial prompt name.
				String initialName = StringUtil.shortenPath(backingFile.getName());
				builder.setInitialFileName(initialName);

				// Mirror the original file type as an extension filter.
				String extension = IOUtil.getExtension(initialName);
				if (extension == null)
					extension = IOUtil.getExtensionFromFileInfo(backingFile);
				if (extension != null)
					builder.setFileExtensionFilter(extension.toUpperCase(), "*." + extension.toLowerCase());
			}
			selectedPath = builder.build().showSaveDialog(null);
		}
		if (selectedPath == null)
			return null;
		String parent = selectedPath.getParent();
		if (parent != null)
			lastWorkspaceExportDir.setValue(parent);
		return selectedPath.toPath().toAbsolutePath().normalize();
	}

	/**
	 * Show an error dialog for a failed workspace export.
	 *
	 * @param failure
	 * 		Failure that occurred during export.
	 */
	private void showWorkspaceExportError(@Nonnull IOException failure) {
		FxThreadUtil.run(() -> ErrorDialogs.show(
				Lang.getBinding("dialog.error.exportworkspace.title"),
				Lang.getBinding("dialog.error.exportworkspace.header"),
				Lang.getBinding("dialog.error.exportworkspace.content"),
				failure
		));
	}

	/**
	 * Export the given class.
	 *
	 * @param classInfo
	 * 		Class to export.
	 */
	public void export(@Nonnull JvmClassInfo classInfo) {
		// Prompt a path for the user to write to.
		ObservableString lastClassExportDir = recentFilesConfig.getLastClassExportDirectory();
		File lastExportDir = lastClassExportDir.unboxingMap(File::new);
		FileChooser chooser = new FileChooserBuilder()
				.setInitialFileName(StringUtil.shortenPath(classInfo.getName()) + ".class")
				.setInitialDirectory(lastExportDir)
				.setFileExtensionFilter("Java Class", "*.class")
				.setTitle(Lang.get("dialog.file.export"))
				.build();
		File selectedPath = chooser.showSaveDialog(null);

		// Selected path is null, meaning user closed out of file chooser.
		// Cancel export.
		if (selectedPath == null)
			return;

		// Update last export dir for classes.
		lastClassExportDir.setValue(selectedPath.getParent());

		// Write to path.
		try {
			Path exportPath = selectedPath.toPath();
			Files.write(exportPath, classInfo.getBytecode());
		} catch (IOException ex) {
			logger.error("Failed to export class to path '{}'", selectedPath, ex);
			ErrorDialogs.show(
					Lang.getBinding("dialog.error.exportclass.title"),
					Lang.getBinding("dialog.error.exportclass.header"),
					Lang.getBinding("dialog.error.exportclass.content"),
					ex
			);
		}
	}

	/**
	 * Export the given file.
	 *
	 * @param fileInfo
	 * 		File to export.
	 */
	public void export(@Nonnull FileInfo fileInfo) {
		// Prompt a path for the user to write to.
		ObservableString lastClassExportDir = recentFilesConfig.getLastClassExportDirectory();
		File lastExportDir = lastClassExportDir.unboxingMap(File::new);
		FileChooser chooser = new FileChooserBuilder()
				.setInitialFileName(StringUtil.shortenPath(fileInfo.getName()))
				.setInitialDirectory(lastExportDir)
				.setTitle(Lang.get("dialog.file.export"))
				.build();
		File selectedPath = chooser.showSaveDialog(null);

		// Selected path is null, meaning user closed out of file chooser.
		// Cancel export.
		if (selectedPath == null)
			return;

		// Write to path.
		try {
			Path exportPath = selectedPath.toPath();
			Files.write(exportPath, fileInfo.getRawContent());
		} catch (IOException ex) {
			logger.error("Failed to export file to path '{}'", selectedPath, ex);
			ErrorDialogs.show(
					Lang.getBinding("dialog.error.exportfile.title"),
					Lang.getBinding("dialog.error.exportfile.header"),
					Lang.getBinding("dialog.error.exportfile.content"),
					ex
			);
		}
	}

	@Nonnull
	private WorkspaceExporter createResourceExporter(@Nonnull WorkspaceResource resource,
	                                                 @Nonnull PathWorkspaceExportConsumer exportConsumer) {
		WorkspaceCompressType compression = exportConfig.getCompression().getValue();
		WorkspaceExportOptions options;
		if (resource instanceof WorkspaceDirectoryResource) {
			options = new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY, exportConsumer);
		} else if (resource instanceof WorkspaceFileResource) {
			options = new WorkspaceExportOptions(compression, WorkspaceOutputType.FILE, exportConsumer);
		} else {
			options = new WorkspaceExportOptions(compression, WorkspaceOutputType.FILE, exportConsumer);
		}
		options.setBundleSupporting(exportConfig.getBundleSupportingResources().getValue());
		options.setCreateZipDirEntries(exportConfig.getCreateZipDirEntries().getValue());
		return options.create();
	}
}
