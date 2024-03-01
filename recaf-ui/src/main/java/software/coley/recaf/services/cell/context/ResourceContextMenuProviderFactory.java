package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link WorkspaceResource} types.
 *
 * @author Matt Coley
 */
public interface ResourceContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		The resource to create a menu for.
	 *
	 * @return Menu provider for the resource.
	 */
	@Nonnull
	default ContextMenuProvider getResourceContextMenuProvider(@Nonnull ContextSource source,
															   @Nonnull Workspace workspace,
															   @Nonnull WorkspaceResource resource) {
		return emptyProvider();
	}
}
