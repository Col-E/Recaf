package software.coley.recaf.workspace;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import software.coley.observables.ObservableString;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.ui.config.ExportConfig;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.io.WorkspaceExportOptions;
import software.coley.recaf.workspace.io.WorkspaceExporter;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Manager module handle exporting {@link Workspace} instances to {@link Path}s.
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
	public void export(Workspace workspace) {
		// Prompt a path for the user to write to.
		WorkspaceResource primaryResource = workspace.getPrimaryResource();
		ObservableString lastWorkspaceExportDir = recentFilesConfig.getLastWorkspaceExportDirectory();
		File lastExportDir = lastWorkspaceExportDir.unboxingMap(File::new);
		File selectedPath;
		if (primaryResource instanceof WorkspaceDirectoryResource) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(lastExportDir);
			chooser.setTitle(Lang.get("dialog.file.export"));
			selectedPath = chooser.showDialog(null);
		} else {
			FileChooser chooser = new FileChooser();
			chooser.setInitialDirectory(lastExportDir);
			chooser.setTitle(Lang.get("dialog.file.export"));
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

		// Create export options from the resource type.
		WorkspaceExportOptions.CompressType compression = exportConfig.getCompression().getValue();
		WorkspaceExportOptions options;
		if (primaryResource instanceof WorkspaceDirectoryResource) {
			options = new WorkspaceExportOptions(WorkspaceExportOptions.OutputType.DIRECTORY, exportPath);
		} else if (primaryResource instanceof WorkspaceFileResource) {
			options = new WorkspaceExportOptions(compression, WorkspaceExportOptions.OutputType.FILE, exportPath);
		} else {
			options = new WorkspaceExportOptions(compression, WorkspaceExportOptions.OutputType.FILE, exportPath);
		}
		options.setBundleSupporting(exportConfig.getBundleSupportingResources().getValue());
		options.setCreateZipDirEntries(exportConfig.getCreateZipDirEntries().getValue());

		// Export the workspace to the selected path.
		WorkspaceExporter exporter = workspaceManager.createExporter(options);
		try {
			exporter.export(workspace);
			logger.info("Exported workspace to path '{}'", exportPath);
		} catch (IOException ex) {
			logger.error("Failed to export workspace to path '{}'", exportPath, ex);
			ErrorDialogs.show(
					Lang.getBinding("dialog.error.exportworkspace.title"),
					Lang.getBinding("dialog.error.exportworkspace.header"),
					Lang.getBinding("dialog.error.exportworkspace.content"),
					ex
			);
		}
	}
}
