package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;

import java.nio.file.Path;

/**
 * A resource sourced from a directory.
 *
 * @author Matt Coley
 */
public interface WorkspaceDirectoryResource extends WorkspaceResource {
	/**
	 * @return Path of the directory the contents of this resource originate from.
	 */
	@Nonnull
	Path getDirectoryPath();
}
