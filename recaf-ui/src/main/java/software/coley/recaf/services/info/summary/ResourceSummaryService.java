package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.Separator;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Provides {@link ResourceSummarizer} content to the {@link WorkspaceInformationPane}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceSummaryService implements Service {
	public static final String SERVICE_ID = "info-summary";
	private static final Logger logger = Logging.get(ResourceSummaryService.class);
	private static final ExecutorService threadPool = ThreadPoolFactory.newSingleThreadExecutor("resource-summary");
	private final Map<String, ResourceSummarizer> summarizers = new ConcurrentHashMap<>();
	private final ResourceSummaryServiceConfig config;

	@Inject
	public ResourceSummaryService(@Nonnull ResourceSummaryServiceConfig config,
	                              @Nonnull Instance<ResourceSummarizer> summarizers) {
		this.config = config;

		// TODO: Summarizer for android
		//  - Manifest
		//    - Entry points (can update existing entry-point summarizer)
		//    - Permissions (and their descriptions, level of concern perhaps?)

		// Add discovered summarizers from classpath.
		for (ResourceSummarizer summarizer : summarizers)
			addSummarizer(summarizer);
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
	 *
	 * @return Future of summarization completion.
	 */
	@Nonnull
	public CompletableFuture<Void> summarizeTo(@Nonnull Workspace workspace,
	                                           @Nonnull WorkspaceResource resource,
	                                           @Nonnull SummaryConsumer consumer) {
		// Run async so we do not block the UI thread
		return CompletableFuture.runAsync(() -> {
			boolean lastSummarizerAppended = false;
			for (ResourceSummarizer summarizer : new TreeSet<>(summarizers.values())) {
				if (lastSummarizerAppended)
					consumer.appendSummary(new Separator());
				try {
					lastSummarizerAppended = summarizer.summarize(workspace, resource, consumer);
				} catch (Throwable t) {
					logger.error("Summarizer '{}' encountered an error", summarizer.getClass().getName(), t);
					lastSummarizerAppended = false;
				}
			}
		}, threadPool);
	}

	/**
	 * @param summarizer
	 * 		Summarizer to add.
	 */
	public void addSummarizer(@Nonnull ResourceSummarizer summarizer) {
		summarizers.put(summarizer.id(), summarizer);
	}

	/**
	 * @param summarizer
	 * 		Summarizer to remove.
	 *
	 * @return {@code true} on removal. {@code false} when it did not exist prior.
	 */
	public boolean removeSummarizer(@Nonnull ResourceSummarizer summarizer) {
		return summarizers.remove(summarizer.id()) != null;
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
