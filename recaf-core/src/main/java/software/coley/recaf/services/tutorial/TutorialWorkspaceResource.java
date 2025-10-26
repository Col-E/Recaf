package software.coley.recaf.services.tutorial;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.resource.BasicWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

/**
 * Specialized tutorial resource.
 *
 * @author Matt Coley
 */
public class TutorialWorkspaceResource extends BasicWorkspaceResource {
	public static final String COMMENT_KEY = "tutorial-resource-key";

	/**
	 * @param builder
	 * 		Resource builder.
	 */
	public TutorialWorkspaceResource(@Nonnull WorkspaceResourceBuilder builder) {
		super(builder);
	}
}
