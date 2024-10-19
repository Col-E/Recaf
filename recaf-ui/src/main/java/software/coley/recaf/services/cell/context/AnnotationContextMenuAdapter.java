package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link AnnotationInfo} types.
 *
 * @author Matt Coley
 */
public interface AnnotationContextMenuAdapter extends ContextMenuAdapter {
	/**
	 * @param menu
	 * 		The menu to adapt.
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
	 * 		The annotation the menu is for.
	 */
	void adaptAnnotationContextMenu(@Nonnull ContextMenu menu,
	                                @Nonnull ContextSource source,
	                                @Nonnull Workspace workspace,
	                                @Nonnull WorkspaceResource resource,
	                                @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                @Nonnull Annotated annotated,
	                                @Nonnull AnnotationInfo annotation);
}
