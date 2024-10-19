package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for contents within an {@link AssemblerPane}.
 *
 * @author Matt Coley
 */
public interface AssemblerContextMenuAdapter extends ContextMenuAdapter {
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
	 * @param declaringClass
	 * 		Containing class.
	 * @param assemblerData
	 * 		The assembler data the menu is for.
	 */
	void adaptAssemblerMenu(@Nonnull ContextMenu menu,
	                        @Nonnull ContextSource source,
	                        @Nonnull Workspace workspace,
	                        @Nonnull WorkspaceResource resource,
	                        @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                        @Nonnull ClassInfo declaringClass,
	                        @Nonnull AssemblerPathData assemblerData);
}
