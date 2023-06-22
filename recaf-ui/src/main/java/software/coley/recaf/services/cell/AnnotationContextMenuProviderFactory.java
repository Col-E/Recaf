package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for {@link AnnotationInfo types}, to be plugged into {@link ContextMenuProviderService}
 * to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface AnnotationContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
	 * @param source
	 * 		Context request origin.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		The annotated item.
	 * @param annotation
	 * 		The annotation to create a menu for.
	 *
	 * @return Menu provider for the method.
	 */
	@Nonnull
	default ContextMenuProvider getAnnotationContextMenuProvider(@Nonnull ContextSource source,
																 @Nonnull Workspace workspace,
																 @Nonnull WorkspaceResource resource,
																 @Nonnull ClassBundle<? extends ClassInfo> bundle,
																 @Nonnull Annotated annotated,
																 @Nonnull AnnotationInfo annotation) {
		return emptyProvider();
	}
}
