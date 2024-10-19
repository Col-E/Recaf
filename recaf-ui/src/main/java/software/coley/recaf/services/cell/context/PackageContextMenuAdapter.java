package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;
import javafx.scene.control.ContextMenu;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu adapter for packages <i>(paths in {@link JvmClassBundle} and {@link AndroidClassBundle})</i>.
 *
 * @author Matt Coley
 */
public interface PackageContextMenuAdapter extends ContextMenuAdapter {
	/**
	 * @param menu
	 * 		The menu to adapt.
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
	 */
	void adaptPackageContextMenu(@Nonnull ContextMenu menu,
	                             @Nonnull ContextSource source,
	                             @Nonnull Workspace workspace,
	                             @Nonnull WorkspaceResource resource,
	                             @Nonnull ClassBundle<? extends ClassInfo> bundle,
	                             @Nonnull String packageName);
}
