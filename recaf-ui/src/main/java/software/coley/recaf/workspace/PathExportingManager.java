package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
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
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manager module handle exporting {@link Info} and {@link Workspace} instances to {@link Path}s.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class PathExportingManager {
	private static final Logger logger = Logging.get(PathExportingManager.class);
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
	}

	/**
	 * Export the current workspace.
	 */
	public void exportCurrent() {
		// Validate current workspace.
		Workspace current = workspaceManager.getCurrent();
		if (current == null) throw new IllegalStateException("Tried to export when no workspace was active!");

		// And export it.
		export(current);
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

		// Prompt a path for the user to write to.
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
			FileChooser chooser = new FileChooserBuilder()
					.setInitialDirectory(lastExportDir)
					.setTitle(Lang.get("dialog.file.export"))
					.build();
			selectedPath = chooser.showSaveDialog(null);
		}

		// Convert selected file to nio path, update last export directory.
		Path exportPath;
		if (selectedPath != null) {
			String parent = selectedPath.getParent();
			if (parent != null) lastWorkspaceExportDir.setValue(parent);
			exportPath = selectedPath.toPath();
		} else {
			// Selected path is null, meaning user closed out of file chooser.
			// Cancel export.
			return;
		}

		// Create export options from the resource type and export the workspace to the selected path.
		WorkspaceExporter exporter = createResourceExporter(primaryResource, exportPath);
		try {
			exporter.export(workspace);
			logger.info("Exported " + contentDescription + " to path '{}'", exportPath);
		} catch (IOException ex) {
			logger.error("Failed to export " + contentDescription + " to path '{}'", exportPath, ex);
			ErrorDialogs.show(
					Lang.getBinding("dialog.error.exportworkspace.title"),
					Lang.getBinding("dialog.error.exportworkspace.header"),
					Lang.getBinding("dialog.error.exportworkspace.content"),
					ex
			);
		}
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
	private WorkspaceExporter createResourceExporter(@Nonnull WorkspaceResource resource, @Nonnull Path path) {
		WorkspaceCompressType compression = exportConfig.getCompression().getValue();
		WorkspaceExportOptions options;
		PathWorkspaceExportConsumer exportConsumer = new PathWorkspaceExportConsumer(path);
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
