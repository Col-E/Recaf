package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.Info;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link Bundle} types.
 *
 * @author Matt Coley
 */
public interface BundleContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		The bundle to create a menu for.
	 *
	 * @return Menu provider for the bundle.
	 */
	@Nonnull
	default ContextMenuProvider getBundleContextMenuProvider(@Nonnull ContextSource source,
															 @Nonnull Workspace workspace,
															 @Nonnull WorkspaceResource resource,
															 @Nonnull Bundle<? extends Info> bundle) {
		return emptyProvider();
	}
}
