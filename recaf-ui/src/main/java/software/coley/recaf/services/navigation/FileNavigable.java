package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;

/**
 * Outline of navigable content representing {@link FileInfo} values.
 *
 * @author Matt Coley
 */
public interface FileNavigable extends Navigable {
	@Nonnull
	@Override
	FilePathNode getPath(); // Force child types to implement getter as FilePathNode
}
