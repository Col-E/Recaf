package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link ClassInfo types}, to be plugged into {@link ContextMenuProviderService}
 * to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface InnerClassContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param outerClass
	 * 		Outer class.
	 * @param inner
	 * 		The inner class to create a menu for.
	 *
	 * @return Menu provider for the inner class.
	 */
	@Nonnull
	default ContextMenuProvider getInnerClassInfoContextMenuProvider(@Nonnull ContextSource source,
																	 @Nonnull Workspace workspace,
																	 @Nonnull WorkspaceResource resource,
																	 @Nonnull ClassBundle<? extends ClassInfo> bundle,
																	 @Nonnull ClassInfo outerClass,
																	 @Nonnull InnerClassInfo inner) {
		return emptyProvider();
	}
}
