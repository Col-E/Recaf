package software.coley.recaf.ui.contextmenu.actions;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.cell.ContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * For simplifying references to methods in {@link ContextMenuProviderFactory} implementations.
 *
 * @param <B>
 * 		Bundle type.
 *
 * @author Matt Coley
 */
public interface DirectoryAction<B extends Bundle<?>> {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param directoryName
	 * 		Target directory.
	 */
	void accept(@Nonnull Workspace workspace,
				@Nonnull WorkspaceResource resource,
				@Nonnull B bundle,
				@Nonnull String directoryName);
}
