package software.coley.recaf.services.info;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Separator;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides {@link ResourceSummarizer} content to the {@link WorkspaceInformationPane}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceSummaryService implements Service {
	public static final String SERVICE_ID = "info-summary";
	private final ResourceSummaryServiceConfig config;
	private final SortedSet<ResourceSummarizer> summarizers = new TreeSet<>();

	@Inject
	public ResourceSummaryService(@Nonnull ResourceSummaryServiceConfig config,
								  @Nonnull Instance<ResourceSummarizer> summarizers) {
		this.config = config;

		// TODO: Summarizer for android
		//  - Manifest
		//  - Permissions

		// Add discovered summarizers from classpath.
		for (ResourceSummarizer summarizer : summarizers)
			this.summarizers.add(summarizer);
	}

	/**
	 * Run all {@link ResourceSummarizer} instances, appending to the given consumer.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to summarize.
	 * @param consumer
	 * 		Consumer of summary data.
	 */
	public void summarizeTo(@Nonnull Workspace workspace,
							@Nonnull WorkspaceResource resource,
							@Nonnull SummaryConsumer consumer) {
		boolean lastSummarizerAppended = true;
		for (ResourceSummarizer summarizer : summarizers) {
			if (lastSummarizerAppended)
				consumer.appendSummary(new Separator());
			lastSummarizerAppended = summarizer.summarize(workspace, resource, consumer);
		}
	}

	/**
	 * @param summarizer
	 * 		Summarizer to add.
	 */
	public void addSummarizer(@Nonnull ResourceSummarizer summarizer) {
		summarizers.add(summarizer);
	}

	/**
	 * @param summarizer
	 * 		Summarizer to remove.
	 *
	 * @return {@code true} on removal. {@code false} when it did not exist prior.
	 */
	public boolean removeSummarizer(@Nonnull ResourceSummarizer summarizer) {
		return summarizers.remove(summarizer);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ResourceSummaryServiceConfig getServiceConfig() {
		return config;
	}
}
