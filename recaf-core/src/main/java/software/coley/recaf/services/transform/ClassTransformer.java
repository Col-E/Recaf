package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Outlines base transformation information such as the identifier and list of any dependencies.
 *
 * @author Matt Coley
 */
public interface ClassTransformer {
	/**
	 * @return Identifier of the transformer.
	 */
	@Nonnull
	String identifier();

	/**
	 * The default key is {@code deobf.<identifier>.description}.
	 *
	 * @return Translation key used for this transformer's localized description.
	 */
	@Nonnull
	default String descriptionKey() {
		return "deobf." + identifier() + ".description";
	}

	/**
	 * @return Java source showing the class before transformation, or {@code null} when no before example is available.
	 */
	@Nullable
	default String beforeExample() {
		return null;
	}

	/**
	 * @return Java source showing the class after transformation, or {@code null} when no after example is available.
	 */
	@Nullable
	default String afterExample() {
		return null;
	}

	/**
	 * @return {@code true} if this transformer should not be applied to following passes if in the current pass it reports no work being done.
	 * {@code false} if this transformer should run in all passes.
	 */
	default boolean pruneAfterNoWork() {
		return false;
	}

	/**
	 * @return Set of transformer classes that are recommended to be run before this one, but not strictly required.
	 *
	 * @see #recommendedSuccessors()
	 * @see #dependencies()
	 */
	@Nonnull
	default Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		return Collections.emptySet();
	}

	/**
	 * @return Set of transformer classes that are recommended to be run after this one, but not strictly required.
	 *
	 * @see #recommendedPredecessors()
	 */
	@Nonnull
	default Set<Class<? extends ClassTransformer>> recommendedSuccessors() {
		return Collections.emptySet();
	}

	/**
	 * @return Set of transformer classes that must run before this one.
	 *
	 * @see #recommendedPredecessors()
	 */
	@Nonnull
	default Set<Class<? extends ClassTransformer>> dependencies() {
		return Collections.emptySet();
	}

	/**
	 * @return Configurable parameters this transformer can read from the {@link TransformationParameters} of a run.
	 */
	@Nonnull
	default List<TransformationParameter<?>> getParameterDefinitions() {
		return Collections.emptyList();
	}
}
