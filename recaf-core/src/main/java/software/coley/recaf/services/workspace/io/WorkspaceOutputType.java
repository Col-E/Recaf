package software.coley.recaf.services.workspace.io;

import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;

/**
 * Output option between single files and directories in {@link WorkspaceExportOptions}.
 *
 * @author Matt Coley
 */
public enum WorkspaceOutputType {
	/**
	 * Output to a single file. The type of which is determined by the primary resource's
	 * {@link WorkspaceFileResource#getFileInfo()} if available. Otherwise, defaults to ZIP/JAR.
	 * <p/>
	 * Delegates to {@link WorkspaceExportConsumer#write(byte[])}
	 */
	FILE,
	/**
	 * Output to a directory.
	 * <p/>
	 * Delegates to {@link WorkspaceExportConsumer#writeRelative(String, byte[])}
	 */
	DIRECTORY
}
