package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalysisResult;
import software.coley.recaf.services.analysis.antitamper.AntiReversalAnalyzer;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.concurrent.Executor;

/**
 * Presents an anti-reversal analysis result in the JavaFX resource summary.
 *
 * @author Matt Coley
 */
public interface AntiReversalResultPresenter extends PrioritySortable {
	/**
	 * @return {@link AntiReversalAnalyzer#getServiceId() Analyzer service ID} handled by this presenter.
	 */
	@Nonnull
	String getAnalyzerId();

	/**
	 * @return {@link AntiReversalAnalyzer#getResultType() Result type} handled by this presenter.
	 */
	@Nonnull
	Class<? extends AntiReversalAnalysisResult> getResultType();

	/**
	 * Checks if the result contains content worth presenting.
	 * <p>
	 * <b>Note:</b>This method runs before JavaFX work is scheduled and must not create JavaFX nodes.
	 *
	 * @param result
	 * 		Result to inspect.
	 *
	 * @return {@code true} when the result should be presented.
	 */
	boolean isApplicable(@Nonnull AntiReversalAnalysisResult result);

	/**
	 * Appends JavaFX controls to the summary pane.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource being summarized.
	 * @param result
	 * 		Result to present.
	 * @param consumer
	 * 		Consumer receiving summary controls.
	 * @param actionExecutor
	 * 		Executor for asynchronous user actions.
	 */
	void appendSummary(@Nonnull Workspace workspace,
	                   @Nonnull WorkspaceResource resource,
	                   @Nonnull AntiReversalAnalysisResult result,
	                   @Nonnull SummaryConsumer consumer,
	                   @Nonnull Executor actionExecutor);
}
