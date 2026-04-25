package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;
import java.util.List;

/**
 * Resource scope for class similarity searches.
 *
 * @param mode
 * 		Scope mode.
 * @param targetResources
 * 		Explicit target resources when using {@link Mode#TARGET_RESOURCES}.
 *
 * @author Matt Coley
 */
public record SimilarClassSearchScope(@Nonnull Mode mode,
                                      @Nonnull List<WorkspaceResource> targetResources) {
	/**
	 * Scope mode.
	 */
	public enum Mode {
		SELF_RESOURCE,
		ALL_NON_INTERNAL,
		TARGET_RESOURCES
	}

	/**
	 * @return Scope limited to the reference class's containing resource.
	 */
	@Nonnull
	public static SimilarClassSearchScope selfResource() {
		return new SimilarClassSearchScope(Mode.SELF_RESOURCE, List.of());
	}

	/**
	 * @return Scope covering all non-internal workspace resources.
	 */
	@Nonnull
	public static SimilarClassSearchScope allNonInternal() {
		return new SimilarClassSearchScope(Mode.ALL_NON_INTERNAL, List.of());
	}

	/**
	 * @param resources
	 * 		Explicit target resources.
	 *
	 * @return Scope covering the given target resources.
	 */
	@Nonnull
	public static SimilarClassSearchScope targetResources(@Nonnull Collection<? extends WorkspaceResource> resources) {
		return new SimilarClassSearchScope(Mode.TARGET_RESOURCES, List.copyOf(resources));
	}
}
