package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service for managing and executing separate anti-reversal analysis processes.
 * <p>
 * Ties into the anti-reversal summarizer to quick user actions to patch anti-reversal techniques.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AntiReversalAnalysisService implements Service {
	public static final String SERVICE_ID = "anti-reversal-analysis";
	private final AntiReversalAnalysisConfig config;
	private final List<AntiReversalAnalyzer<?>> analyzers = new ArrayList<>();

	@Inject
	public AntiReversalAnalysisService(@Nonnull AntiReversalAnalysisConfig config,
	                                   @Nonnull Instance<AntiReversalAnalyzer<?>> analyzers) {
		this.config = config;

		// Register core analyzers.
		for (AntiReversalAnalyzer<?> analyzer : analyzers)
			registerAnalyzer(analyzer);
	}

	/**
	 * Register an analyzer with the service.
	 *
	 * @param analyzer
	 * 		Analyzer to register.
	 *
	 * @throws IllegalArgumentException
	 * 		When an analyzer with the same service ID is already registered.
	 */
	public void registerAnalyzer(@Nonnull AntiReversalAnalyzer<?> analyzer) {
		if (analyzers.stream().anyMatch(a -> a.getServiceId().equals(analyzer.getServiceId())))
			throw new IllegalArgumentException("Analyzer with service ID already registered: " + analyzer.getServiceId());
		analyzers.add(analyzer);
		analyzers.sort(Comparator.comparing(AntiReversalAnalyzer::getServiceId));
	}

	/**
	 * @return Registered analyzers, ordered by analyzer ID.
	 */
	@Nonnull
	public List<AntiReversalAnalyzer<?>> getAnalyzers() {
		return analyzers;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to analyze.
	 * @param analyzerType
	 * 		Analyzer type.
	 * @param <R>
	 * 		Result type.
	 *
	 * @return Analyzer result.
	 *
	 * @throws IllegalArgumentException
	 * 		When the analyzer type is unknown.
	 */
	@Nonnull
	public <R extends AntiReversalAnalysisResult> R analyze(@Nonnull Workspace workspace,
	                                                        @Nonnull WorkspaceResource resource,
	                                                        @Nonnull Class<? extends AntiReversalAnalyzer<R>> analyzerType) {
		for (AntiReversalAnalyzer<?> analyzer : analyzers)
			if (analyzerType.isInstance(analyzer))
				return castAndAnalyze(analyzer, workspace, resource);
		throw new IllegalArgumentException("Unknown analyzer type: " + analyzerType.getName());
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public AntiReversalAnalysisConfig getServiceConfig() {
		return config;
	}

	@SuppressWarnings("unchecked")
	private static <R extends AntiReversalAnalysisResult> R castAndAnalyze(@Nonnull AntiReversalAnalyzer<?> analyzer,
	                                                                       @Nonnull Workspace workspace,
	                                                                       @Nonnull WorkspaceResource resource) {
		return (R) analyzer.analyze(workspace, resource);
	}
}
