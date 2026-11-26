package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Ordered sequence of transformation phases.
 *
 * @param phases
 * 		Phases that make up the plan, executed in declaration order.
 *
 * @author Matt Coley
 */
public record TransformationPlan(@Nonnull List<TransformationPhase> phases) {}
