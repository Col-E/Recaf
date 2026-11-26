package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Aggregate result for an ordered sequence of JVM transformation phases.
 * <p>
 * Every phase except the last is applied to the workspace as execution progresses,
 * so the caller only needs to commit the final phase via {@link #apply()}.
 *
 * @param phaseResults
 * 		Phase results in execution order, including skipped phases.
 *
 * @author Matt Coley
 */
public record TransformationPlanResult(@Nonnull List<TransformationPhaseResult> phaseResults) {
	/**
	 * @return {@code true} when every phase ended with a successful status.
	 */
	public boolean isComplete() {
		return phaseResults.stream().allMatch(result -> result.status().isSuccess());
	}

	/**
	 * Applies the final phase's result to the workspace.
	 * <p>
	 * Earlier phases were already applied during execution, and aborted plans never
	 * commit the failed or cancelled phase, so this method intentionally does nothing
	 * when {@link #isComplete()} is {@code false}.
	 */
	public void apply() {
		if (!isComplete())
			return;

		TransformationPhaseResult last = phaseResults.getLast();
		if (last.result() != null)
			last.result().apply();
	}
}
