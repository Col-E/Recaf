package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;

/**
 * One step in an ordered {@link TransformationPlan}.
 *
 * @param id
 * 		Unique phase identifier.
 * @param maxPasses
 * 		Maximum number of passes for the phase, at least one.
 * @param jvmTransformers
 * 		JVM transformer classes to run in the phase.
 * @param parameters
 * 		Parameters supplied to transformers in the phase.
 *
 * @author Matt Coley
 */
public record TransformationPhase(@Nonnull String id,
                                  int maxPasses,
                                  @Nonnull List<Class<? extends JvmClassTransformer>> jvmTransformers,
                                  @Nonnull TransformationParameters parameters) {
	@SafeVarargs
	public TransformationPhase(@Nonnull String id, int maxPasses, @Nonnull Class<? extends JvmClassTransformer>... transformers) {
		this(id, maxPasses, Arrays.asList(transformers), TransformationParameters.empty());
	}
}
