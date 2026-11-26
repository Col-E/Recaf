package software.coley.recaf.services.info.summary.builtin;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisResult;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisService;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalyzer;
import software.coley.recaf.services.info.summary.AntiReversalResultPresenter;
import software.coley.recaf.services.info.summary.AntiReversalResultPresenterService;
import software.coley.recaf.services.info.summary.ResourceSummarizer;
import software.coley.recaf.services.info.summary.SummaryConsumer;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Summarizer that allows patching of common anti-decompilation tricks.
 *
 * @author Matt Coley
 * @see AntiReversalAnalysisService
 * @see AntiReversalResultPresenterService
 */
@ApplicationScoped
public class AntiDecompilationSummarizer implements ResourceSummarizer {
	private static final Logger logger = Logging.get(AntiDecompilationSummarizer.class);
	private final AntiReversalAnalysisService antiReversalAnalysisService;
	private final AntiReversalResultPresenterService presenterService;

	@Inject
	public AntiDecompilationSummarizer(@Nonnull AntiReversalAnalysisService antiReversalAnalysisService,
	                                   @Nonnull AntiReversalResultPresenterService presenterService) {
		this.antiReversalAnalysisService = antiReversalAnalysisService;
		this.presenterService = presenterService;
	}

	@Override
	public boolean summarize(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource resource,
	                         @Nonnull SummaryConsumer consumer) {
		// Collect all analyzers by their service ID so we can match them to presenters.
		Map<String, AntiReversalAnalyzer<?>> analyzers = new HashMap<>();
		for (AntiReversalAnalyzer<?> analyzer : antiReversalAnalysisService.getAnalyzers())
			analyzers.put(analyzer.getServiceId(), analyzer);

		// Collect all presenters that are applicable to the resource.
		List<Presentation> presentations = new ArrayList<>();
		for (AntiReversalResultPresenter presenter : presenterService.getPresenters()) {
			// Skip if the presenter has no analyzer registered for it.
			AntiReversalAnalyzer<?> analyzer = analyzers.get(presenter.getAnalyzerId());
			if (analyzer == null) {
				logger.warn("Cannot present anti-reversal result: analyzer '{}' is not registered", presenter.getAnalyzerId());
				continue;
			}

			// Add presentation if the presenter is applicable to the resource.
			try {
				AntiReversalAnalysisResult result = analyze(workspace, resource, analyzer);
				if (!presenter.getResultType().isInstance(result)) {
					logger.warn("Cannot present anti-reversal result from '{}': expected '{}', got '{}'",
							presenter.getAnalyzerId(), presenter.getResultType().getName(), result.getClass().getName());
					continue;
				}
				if (presenter.isApplicable(result))
					presentations.add(new Presentation(presenter, result));
			} catch (Throwable t) {
				logger.error("Anti-reversal presenter '{}' encountered an error", presenter.getAnalyzerId(), t);
			}
		}

		// Skip if no registered presenter found applicable anti-reversal work.
		if (presentations.isEmpty())
			return false;

		// We have actions to take, create UI to apply patches.
		FxThreadUtil.run(() -> {
			ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("anti-decompile-patching");
			Label title = new BoundLabel(Lang.getBinding("service.analysis.anti-decompile"));
			title.getStyleClass().addAll(Styles.TITLE_4);
			consumer.appendSummary(title);
			for (Presentation presentation : presentations)
				presentation.presenter().appendSummary(workspace, resource, presentation.result(), consumer, service);
		});
		return true;
	}

	@Override
	public int getPriority() {
		return PRIORITY_ANTI_DECOMPILATION;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Nonnull
	private AntiReversalAnalysisResult analyze(@Nonnull Workspace workspace,
	                                           @Nonnull WorkspaceResource resource,
	                                           @Nonnull AntiReversalAnalyzer<?> analyzer) {
		return antiReversalAnalysisService.analyze(workspace, resource, (AntiReversalAnalyzer) analyzer);
	}

	private record Presentation(@Nonnull AntiReversalResultPresenter presenter,
	                            @Nonnull AntiReversalAnalysisResult result) {}
}
