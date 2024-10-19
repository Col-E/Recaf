package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link InnerClassInfo} types.
 *
 * @author Matt Coley
 */
public interface InnerClassContextMenuAdapter extends ContextMenuAdapter {
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
	 * @param outerClass
	 * 		Outer class.
	 * @param inner
	 * 		The inner class the menu is for.
	 */
	void adaptInnerClassInfoContextMenu(@Nonnull ContextMenu menu,
	                                    @Nonnull ContextSource source,
	                                    @Nonnull Workspace workspace,
	                                    @Nonnull WorkspaceResource resource,
	                                    @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                    @Nonnull ClassInfo outerClass,
	                                    @Nonnull InnerClassInfo inner);
}
