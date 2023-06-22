package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link ClassInfo types}, to be plugged into {@link ContextMenuProviderService}
 * to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface ClassContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 *
	 * @return Menu provider for the class.
	 */
	@Nonnull
	default ContextMenuProvider getJvmClassInfoContextMenuProvider(@Nonnull ContextSource source,
																   @Nonnull Workspace workspace,
																   @Nonnull WorkspaceResource resource,
																   @Nonnull JvmClassBundle bundle,
																   @Nonnull JvmClassInfo info) {
		return emptyProvider();
	}

	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		The class to create a menu for.
	 *
	 * @return Menu provider for the class.
	 */
	@Nonnull
	default ContextMenuProvider getAndroidClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	   @Nonnull Workspace workspace,
																	   @Nonnull WorkspaceResource resource,
																	   @Nonnull AndroidClassBundle bundle,
																	   @Nonnull AndroidClassInfo info) {
		return emptyProvider();
	}
}
