package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;

/**
 * Listener for handling updates to {@link AndroidClassInfo} values within a {@link AndroidClassBundle}
 * contained in a {@link WorkspaceResource}.
 *
 * @author Matt Coley
 */
public interface ResourceAndroidClassListener extends PrioritySortable {
	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param cls
	 * 		The new class.
	 */
	void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls);

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
	void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle,
					   @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls);

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param cls
	 * 		The removed class.
	 */
	void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls);
}
