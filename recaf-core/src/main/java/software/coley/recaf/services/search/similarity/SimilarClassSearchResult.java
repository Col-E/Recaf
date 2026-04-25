package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;

/**
 * Result of a similar class search.
 *
 * @param path
 * 		Path to matched class.
 * @param similarity
 * 		Overall similarity score in {@code [0.0, 1.0]}.
 * @param breakdown
 * 		Structured scoring breakdown.
 *
 * @author Matt Coley
 */
public record SimilarClassSearchResult(@Nonnull ClassPathNode path,
                                       double similarity,
                                       @Nonnull SimilarClassScoreBreakdown breakdown) {}
