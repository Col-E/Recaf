package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.ui.pane.editing.assembler.AssemblerPane;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Context menu provider for contents within an {@link AssemblerPane}, to be plugged into {@link ContextMenuProviderService}
 * to allow for third party menu customization.
 *
 * @author Matt Coley
 */
public interface AssemblerContextMenuProviderFactory extends ContextMenuProviderFactory {
	/**
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
	 * 		The assembler data to create a menu for.
	 *
	 * @return Menu provider for the selected contents in the assembler.
	 */
	@Nonnull
	default ContextMenuProvider getProvider(@Nonnull ContextSource source,
											@Nonnull Workspace workspace,
											@Nonnull WorkspaceResource resource,
											@Nonnull ClassBundle<? extends ClassInfo> bundle,
											@Nonnull ClassInfo declaringClass,
											@Nonnull AssemblerPathData assemblerData) {
		return emptyProvider();
	}
}
