package software.coley.recaf.services.tutorial;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.BasicWorkspace;

/**
 * Specialized tutorial workspace.
 *
 * @author Matt Coley
 */
public class TutorialWorkspace extends BasicWorkspace {
	/**
	 * @param primary
	 * 		Tutorial resource.
	 */
	public TutorialWorkspace(@Nonnull TutorialWorkspaceResource primary) {
		super(primary);
	}
}
