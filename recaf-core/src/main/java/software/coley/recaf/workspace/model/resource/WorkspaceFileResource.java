package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;

/**
 * A resource sourced from a file.
 *
 * @author Matt Coley
 */
public interface WorkspaceFileResource extends WorkspaceResource {
	/**
	 * @return Information about the file loaded from.
	 */
	@Nonnull
	FileInfo getFileInfo();
}
