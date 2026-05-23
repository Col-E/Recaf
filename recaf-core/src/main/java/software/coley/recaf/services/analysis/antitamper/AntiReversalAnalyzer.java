package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Individual anti-reversal analysis process.
 *
 * @param <R>
 * 		Result type.
 *
 * @author Matt Coley
 */
public interface AntiReversalAnalyzer<R extends AntiReversalAnalysisResult> {
	/**
	 * @return Stable analyzer identifier.
	 */
	@Nonnull
	String getServiceId();

	/**
	 * @return Result type produced by the analyzer.
	 */
	@Nonnull
	Class<R> getResultType();

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect.
	 *
	 * @return Analysis result.
	 */
	@Nonnull
	R analyze(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource);
}
