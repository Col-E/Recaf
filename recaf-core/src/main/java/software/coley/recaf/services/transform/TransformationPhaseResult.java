package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Result and lifecycle state for one phase execution.
 *
 * @param phase
 * 		Phase described by this result.
 * @param result
 * 		Low-level JVM transformation result, or {@code null} if the phase never ran
 * 		<i>({@link Status#SKIPPED} or pre-start {@link Status#CANCELLED})</i>.
 * @param status
 * 		Outcome of the phase execution.
 * @param passes
 * 		Number of passes executed for the phase.
 *
 * @author Matt Coley
 */
public record TransformationPhaseResult(@Nonnull TransformationPhase phase,
                                        @Nullable JvmTransformResult result,
                                        @Nonnull Status status,
                                        int passes) {
	/**
	 * Outcome of a phase execution.
	 */
	public enum Status {
		/** Phase ended because no transformer reported work on a full pass. */
		STABLE,
		/** Phase ended because the pass bound was reached with work remaining. */
		MAX_PASSES,
		/** At least one transformer failed; phase output is never committed. */
		FAILED,
		/** Feedback cancelled the phase; phase output is never committed. */
		CANCELLED,
		/** Phase never started because an earlier phase stopped the plan. */
		SKIPPED;

		/**
		 * @return {@code true} for {@link #STABLE} and {@link #MAX_PASSES}.
		 */
		public boolean isSuccess() {
			return this == STABLE || this == MAX_PASSES;
		}
	}
}
