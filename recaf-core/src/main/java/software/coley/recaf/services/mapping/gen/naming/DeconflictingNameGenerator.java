package software.coley.recaf.services.mapping.gen.naming;

import jakarta.annotation.Nullable;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Name generation outline that supports deconflicting cases where two items may create the same name.
 *
 * @author Matt Coley
 */
public interface DeconflictingNameGenerator extends NameGenerator {
	/**
	 * Enables name deconfliction.
	 *
	 * @param workspace
	 * 		Workspace to assign, to deconflict names.
	 */
	void setWorkspace(@Nullable Workspace workspace);
}
