package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.FileBundle;

/**
 * Listener for handling updates to {@link FileInfo} values within a {@link FileBundle}
 * contained in a {@link WorkspaceResource}.
 *
 * @author Matt Coley
 */
public interface ResourceFileListener extends PrioritySortable {
	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param file
	 * 		The new file.
	 */
	void onNewFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file);

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param oldFile
	 * 		The old file value.
	 * @param newFile
	 * 		The new file value.
	 */
	void onUpdateFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle,
					  @Nonnull FileInfo oldFile, @Nonnull FileInfo newFile);

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param file
	 * 		The removed file.
	 */
	void onRemoveFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file);
}
