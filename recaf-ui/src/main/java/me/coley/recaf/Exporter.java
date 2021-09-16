package me.coley.recaf;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

/**
 * Export utility. Handles various cases and configs for exporting the primary resource.
 *
 * @author Matt Coley
 */
public class Exporter {
	private static final Logger logger = Logging.get(Exporter.class);

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
			StringWriter trace = new StringWriter();
			new Throwable("").printStackTrace(new PrintWriter(trace));
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
		// TODO: Write the primary jar to the given path. Some things to consider:
		//  - Toggle for ZIP compression: https://github.com/Col-E/Recaf/issues/401
		//  - Some files may be prefixed
		//    - War:    "WEB-INF/classes"
		//    - Spring: "BOOT-INF/classes"
		//  - Zip entry metadata like comments should be transferred
		//    - Need to setup metadata tracking first
		//    - Each file/class entry should be capable of tracking info even with mappings applied
	}
}
