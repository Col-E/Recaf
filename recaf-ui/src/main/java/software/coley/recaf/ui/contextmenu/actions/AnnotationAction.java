package software.coley.recaf.ui.contextmenu.actions;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.services.cell.context.ContextMenuProviderFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * For simplifying references to annotation entries in {@link ContextMenuProviderFactory} implementations.
 *
 * @author Matt Coley
 */
public interface AnnotationAction {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param annotated
	 * 		Item declared as the annotation target.
	 * @param annotation
	 * 		Target annotation.
	 */
	void accept(@Nonnull Workspace workspace,
				@Nonnull WorkspaceResource resource,
				@Nonnull ClassBundle<? extends ClassInfo> bundle,
				@Nonnull Annotated annotated,
				@Nonnull AnnotationInfo annotation);
}
