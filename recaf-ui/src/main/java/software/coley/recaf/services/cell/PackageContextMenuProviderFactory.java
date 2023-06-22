package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for packages <i>(paths in {@link JvmClassBundle} and {@link AndroidClassBundle})</i>,
 * to be plugged into {@link ContextMenuProviderService} to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface PackageContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request source.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param packageName
	 * 		The full package name, separated by {@code /}.
	 *
	 * @return Menu provider for the package.
	 */
	@Nonnull
	default ContextMenuProvider getPackageContextMenuProvider(@Nonnull ContextSource source,
															  @Nonnull Workspace workspace,
															  @Nonnull WorkspaceResource resource,
															  @Nonnull ClassBundle<? extends ClassInfo> bundle,
															  @Nonnull String packageName) {
		return emptyProvider();
	}
}
