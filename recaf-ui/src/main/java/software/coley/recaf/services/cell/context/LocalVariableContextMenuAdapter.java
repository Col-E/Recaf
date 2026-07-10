package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.LocalVariable;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for {@link LocalVariable} types.
 *
 * @author Matt Coley
 */
public interface LocalVariableContextMenuAdapter extends ContextMenuAdapter {
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
	 * @param declaringMethod
	 * 		Containing method.
	 * @param variable
	 * 		The variable the menu is for.
	 */
	void adaptLocalVariableContextMenu(@Nonnull ContextMenu menu,
	                                   @Nonnull ContextSource source,
	                                   @Nonnull Workspace workspace,
	                                   @Nonnull WorkspaceResource resource,
	                                   @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                                   @Nonnull ClassInfo declaringClass,
	                                   @Nonnull MethodMember declaringMethod,
	                                   @Nonnull LocalVariable variable);
}
