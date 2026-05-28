package software.coley.recaf.services.analysis.structure;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AreaAnalysisService}.
 * <p>
 * These values control how aggressively classes are grouped into areas after SCC detection.
 * <p>
 * Example:
 * <pre>
 * A <-> B
 * Main -> Helper
 * </pre>
 * {@code A/B} is an SCC. {@code Helper} may merge into {@code Main}'s area depending on the merge settings.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AreaAnalysisConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean includeExternalReferenceScores = new ObservableBoolean(true);
	private final ObservableBoolean mergeSccGroups = new ObservableBoolean(true);
	private final ObservableInteger maxMergedChildSize = new ObservableInteger(10);
	private final ObservableInteger maxMerges = new ObservableInteger(1000);
	private final ObservableInteger spaghettiThresholdPercent = new ObservableInteger(75);

	@Inject
	public AreaAnalysisConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, AreaAnalysisService.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("include-external-reference-scores", boolean.class, includeExternalReferenceScores));
		addValue(new BasicConfigValue<>("merge-scc-groups", boolean.class, mergeSccGroups));
		addValue(new BasicConfigValue<>("max-merged-child-size", int.class, maxMergedChildSize));
		addValue(new BasicConfigValue<>("max-merge-operations", int.class, maxMerges));
		addValue(new BasicConfigValue<>("spaghetti-threshold-percent", int.class, spaghettiThresholdPercent));
	}

	/**
	 * Controls whether references to classes outside the analyzed resource scope affect merge scoring.
	 * <p>
	 * This does not create groups for external classes. It only changes how strongly an in-scope class appears
	 * tied to code outside the analyzed resource.
	 * <p>
	 * Example:
	 * <pre>
	 * HttpFacade -> okhttp3/OkHttpClient
	 * HttpFacade -> InternalHelper
	 * </pre>
	 * When enabled, framework usage counts toward external connectivity. When disabled, only in-scope edges
	 * affect grouping decisions.
	 *
	 * @return {@code true} to include external reference scores, {@code false} to ignore them.
	 */
	@Nonnull
	public ObservableBoolean getIncludeExternalReferenceScores() {
		return includeExternalReferenceScores;
	}

	/**
	 * Controls whether the post-SCC merge pass is enabled.
	 * <p>
	 * When enabled, small one-way-dependent SCCs may be folded into a nearby parent area. When disabled,
	 * results stay closer to raw SCCs.
	 * Example:
	 * <pre>
	 * Main -> Helper
	 * </pre>
	 * With this enabled, {@code Helper} may merge into {@code Main}. With it disabled, they remain separate.
	 *
	 * @return {@code true} for SCC merge pass, {@code false} to keep SCC groups separate.
	 */
	@Nonnull
	public ObservableBoolean getMergeSccGroups() {
		return mergeSccGroups;
	}

	/**
	 * Maximum size of a candidate child SCC/group that may be merged into another group during the merge pass.
	 * <p>
	 * Larger values allow bigger helper-like groups to be merged. Smaller values keep merges conservative.
	 * <p>
	 * Example:
	 * <pre>
	 * Main -> Helper1
	 * Main -> Helper2
	 * Helper1 <-> Helper2
	 * </pre>
	 * The helper pair is size 2. With a limit of {@code 1} it cannot merge. With {@code 3} it may.
	 *
	 * @return Maximum mergeable child size.
	 */
	@Nonnull
	public ObservableInteger getMaxMergedChildSize() {
		return maxMergedChildSize;
	}

	/**
	 * Maximum number of SCC merge operations allowed during the merge pass.
	 * This is a safety limit to prevent excessive computations on large workspaces.
	 *
	 * @return Maximum number of merge operations allowed during the merge pass.
	 */
	@Nonnull
	public ObservableInteger getMaxMerges() {
		return maxMerges;
	}

	/**
	 * Percentage threshold used to flag a dominant group as spaghetti.
	 * <p>
	 * Lower values trigger the warning more often. Higher values only flag very dominant groups.
	 * <p>
	 * Example:
	 * <br>
	 * In a 10-class resource, a threshold of {@code 75} flags a 9-class dominant group.
	 * A threshold of {@code 90} is stricter.
	 *
	 * @return Dominant-group percentage threshold, in range of {@code 0-100}.
	 */
	@Nonnull
	public ObservableInteger getSpaghettiThresholdPercent() {
		return spaghettiThresholdPercent;
	}
}
