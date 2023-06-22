package software.coley.recaf.services.info;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Provides summary information about a {@link WorkspaceResource} to the user.
 *
 * @author Matt Coley
 */
public interface ResourceSummarizer extends Comparable<ResourceSummarizer> {
	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to summarize.
	 * @param consumer
	 * 		Consumer of summary data.
	 *
	 * @return {@code true} when data was summarized.
	 * {@code false} when summarization was skipped.
	 */
	boolean summarize(@Nonnull Workspace workspace,
				   @Nonnull WorkspaceResource resource,
				   @Nonnull SummaryConsumer consumer);

	/**
	 * @return Summarizer identity.
	 */
	@Nonnull
	default String id() {
		return getClass().getSimpleName();
	}

	@Override
	default int compareTo(ResourceSummarizer o) {
		return id().compareTo(o.id());
	}
}
