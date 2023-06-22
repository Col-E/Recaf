package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Condition applied to {@link WorkspaceManager} to prevent closure of an active workspace for when
 * {@link WorkspaceManager#setCurrent(Workspace)} is called.
 *
 * @author Matt Coley
 */
public interface WorkspaceCloseCondition {
	/**
	 * @param current
	 * 		Current workspace.
	 *
	 * @return {@code true} when the operation is allowed.
	 */
	boolean canClose(@Nonnull Workspace current);
}