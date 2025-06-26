package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Provides summary information about a {@link WorkspaceResource} to the user.
 *
 * @author Matt Coley
 */
public interface ResourceSummarizer extends Comparable<ResourceSummarizer> {
	/**
	 * Computes summary information about the given workspace.
	 * <p/>
	 * When implementations of this method need to display output
	 * all UI creation should be wrapped in {@link FxThreadUtil#run(Runnable)}.
	 *
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
