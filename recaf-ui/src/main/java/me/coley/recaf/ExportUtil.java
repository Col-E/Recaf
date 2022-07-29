package me.coley.recaf;

import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.ExportConfig;
import me.coley.recaf.util.UncheckedRunnable;
import me.coley.recaf.util.Exporter;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.source.ApkContentSource;
import me.coley.recaf.workspace.resource.source.ClassContentSource;
import me.coley.recaf.workspace.resource.source.DirectoryContentSource;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * Export utility. Handles various cases and configs for exporting the primary resource.
 *
 * @author Matt Coley
 */
public class ExportUtil {
	private static final Logger logger = Logging.get(ExportUtil.class);

	/**
	 * Writes the primary jar of the current workspace to the given path.
	 *
	 * @param path
	 * 		Path to write to.
	 */
	public static void write(Path path) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			// This should not occur, so we want to be able to figure out and remove cases where it may occur.
			String trace = StringUtil.traceToString(new Throwable());
			logger.warn("Tried to export application, but no workspace was loaded! Context: {}", trace);
			return;
		}
		write(path, workspace);
	}

	/**
	 * Writes the primary resource of the given workspace to the given path.
	 *
	 * @param path
	 * 		Path to write to.
	 * @param workspace
	 * 		Workspace with primary resource to write.
	 */
	public static void write(Path path, Workspace workspace) {
		logger.info("Preparing to write to: {}", path);
		ExportConfig config = Configs.export();
		Exporter exporter = new Exporter(path);
		exporter.shadeLibs = config.shadeLibs;
		exporter.compress = config.compress;
		Resource resource = workspace.getResources().getPrimary();
		UncheckedRunnable exportProcess;
		if (resource.getContentSource() instanceof ClassContentSource && !exporter.shadeLibs) {
			exportProcess = exporter::writeAsSingleFile;
		} else if (resource.getContentSource() instanceof DirectoryContentSource) {
			exportProcess = exporter::writeAsDirectory;
		} else if (resource.getContentSource() instanceof ApkContentSource) {
			exportProcess = exporter::writeAsAPK;
		} else {
			exportProcess = exporter::writeAsArchive;
		}
		exporter.addWorkspace(workspace);
		try {
			exportProcess.run();
		} catch (Throwable e) {
			logger.error("Failed to export workspace!", e);
		}
	}
}
