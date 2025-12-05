package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

/**
 * Listener for handling updates to {@link JvmClassInfo} values within a {@link JvmClassBundle}
 * contained in a {@link WorkspaceResource}.
 *
 * @author Matt Coley
 */
public interface ResourceJvmClassListener extends PrioritySortable {
	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param cls
	 * 		The new class.
	 */
	void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls);

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param oldCls
	 * 		The old class value.
	 * @param newCls
	 * 		The new class value.
	 */
	void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
					   @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls);

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param cls
	 * 		The removed class.
	 */
	void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls);
}
