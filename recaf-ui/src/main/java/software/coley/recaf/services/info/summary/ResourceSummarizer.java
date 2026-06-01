package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.services.info.summary.builtin.AndroidPermissionSummarizer;
import software.coley.recaf.services.info.summary.builtin.AntiDecompilationSummarizer;
import software.coley.recaf.services.info.summary.builtin.AreaAnalysisSummarizer;
import software.coley.recaf.services.info.summary.builtin.EntryPointSummarizer;
import software.coley.recaf.services.info.summary.builtin.HashSummarizer;
import software.coley.recaf.services.info.summary.builtin.JarSigningSummarizer;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;

/**
 * Provides summary information about a {@link WorkspaceResource} to the user.
 *
 * @author Matt Coley
 */
public interface ResourceSummarizer extends PrioritySortable {
	/**
	 * Comparator for sorting summarizers by their ID.
	 */
	Comparator<ResourceSummarizer> ID_COMPARATOR = Comparator.comparing(ResourceSummarizer::id);

	/** @see AndroidPermissionSummarizer */
	int PRIORITY_ANDROID_PERMISSIONS = 3000;
	/** @see AntiDecompilationSummarizer */
	int PRIORITY_ANTI_DECOMPILATION = 1000;
	/** @see AreaAnalysisSummarizer */
	int PRIORITY_AREA_ANALYSIS = 2000;
	/** @see EntryPointSummarizer */
	int PRIORITY_ENTRY_POINT = 0;
	/** @see HashSummarizer */
	int PRIORITY_FILE_HASH = 4000;
	/** @see JarSigningSummarizer */
	int PRIORITY_JAR_SIGNATURES = 5000;

	/**
	 * Computes summary information about the given workspace.
	 * <p>
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
	default int compareTo(PrioritySortable o) {
		// Sort by priority first, then for any ties break the tie by ID.
		int cmp = PrioritySortable.super.compareTo(o);
		if (cmp == 0) cmp = ID_COMPARATOR.compare(this, (ResourceSummarizer) o);
		return cmp;
	}
}
