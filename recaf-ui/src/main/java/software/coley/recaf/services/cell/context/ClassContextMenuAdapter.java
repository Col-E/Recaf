package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link ClassInfo} types.
 *
 * @author Matt Coley
 */
public interface ClassContextMenuAdapter extends ContextMenuAdapter {
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
	 * @param info
	 * 		The class the menu is for.
	 */
	default void adaptJvmClassMenu(@Nonnull ContextMenu menu,
								   @Nonnull ContextSource source,
								   @Nonnull Workspace workspace,
								   @Nonnull WorkspaceResource resource,
								   @Nonnull JvmClassBundle bundle,
								   @Nonnull JvmClassInfo info) {}

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
	 * @param info
	 * 		The class the menu is for.
	 */
	default void adaptAndroidClassMenu(@Nonnull ContextMenu menu,
									   @Nonnull ContextSource source,
									   @Nonnull Workspace workspace,
									   @Nonnull WorkspaceResource resource,
									   @Nonnull AndroidClassBundle bundle,
									   @Nonnull AndroidClassInfo info) {}
}
