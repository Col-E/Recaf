package software.coley.recaf.services.tutorial;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;
import software.coley.recaf.workspace.model.BasicWorkspace;

/**
 * Specialized tutorial workspace.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Tutorial is for UI usage only, and is not testable in a meaningful way.")
public class TutorialWorkspace extends BasicWorkspace {
	/**
	 * @param primary
	 * 		Tutorial resource.
	 */
	public TutorialWorkspace(@Nonnull TutorialWorkspaceResource primary) {
		super(primary);
	}
}
