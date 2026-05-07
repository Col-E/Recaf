package software.coley.recaf.services.mapping.matching;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.mapping.IntermediateMappings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Report of similarity-based mappings and accepted class matches.
 *
 * @author Matt Coley
 */
public class SimilarityMappingsReport {
	private final IntermediateMappings mappings;
	private final List<SimilarityClassMatch> acceptedMatches;
	private final List<ClassPathNode> unresolvedClasses;
	private final Map<String, SimilarityClassMatch> acceptedMatchesBySourceName;

	/**
	 * @param mappings
	 * 		Generated mappings.
	 * @param acceptedMatches
	 * 		Accepted class matches.
	 * @param unresolvedClasses
	 * 		Source classes that did not receive an accepted match.
	 */
	public SimilarityMappingsReport(@Nonnull IntermediateMappings mappings,
	                                @Nonnull List<SimilarityClassMatch> acceptedMatches,
	                                @Nonnull List<ClassPathNode> unresolvedClasses) {
		this.mappings = Objects.requireNonNull(mappings);
		this.acceptedMatches = List.copyOf(acceptedMatches);
		this.unresolvedClasses = List.copyOf(unresolvedClasses);

		Map<String, SimilarityClassMatch> acceptedMatchesBySourceName = new TreeMap<>();
		for (SimilarityClassMatch acceptedMatch : acceptedMatches)
			acceptedMatchesBySourceName.put(acceptedMatch.sourcePath().getValue().getName(), acceptedMatch);
		this.acceptedMatchesBySourceName = Map.copyOf(acceptedMatchesBySourceName);
	}

	/**
	 * @return Generated mappings.
	 */
	@Nonnull
	public IntermediateMappings getMappings() {
		return mappings;
	}

	/**
	 * @return Accepted class matches.
	 */
	@Nonnull
	public List<SimilarityClassMatch> getAcceptedMatches() {
		return acceptedMatches;
	}

	/**
	 * @return Source classes that did not receive an accepted match.
	 */
	@Nonnull
	public List<ClassPathNode> getUnresolvedClasses() {
		return unresolvedClasses;
	}

	/**
	 * @param sourceClassName
	 * 		Source class name.
	 *
	 * @return Accepted match for the source class, or {@code null}.
	 */
	@Nullable
	public SimilarityClassMatch getAcceptedMatch(@Nonnull String sourceClassName) {
		return acceptedMatchesBySourceName.get(sourceClassName);
	}

	/**
	 * @param sourcePath
	 * 		Source class path.
	 *
	 * @return Accepted match for the source class, or {@code null}.
	 */
	@Nullable
	public SimilarityClassMatch getAcceptedMatch(@Nonnull ClassPathNode sourcePath) {
		return getAcceptedMatch(sourcePath.getValue().getName());
	}
}
