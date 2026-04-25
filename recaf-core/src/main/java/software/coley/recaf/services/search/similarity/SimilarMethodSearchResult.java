package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassMemberPathNode;

/**
 * Result of a similar method search.
 *
 * @param path
 * 		Path to the matched method.
 * @param similarity
 * 		Normalized similarity score in {@code [0.0, 1.0]}.
 *
 * @author Matt Coley
 */
public record SimilarMethodSearchResult(@Nonnull ClassMemberPathNode path, double similarity) {}
