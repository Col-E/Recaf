package software.coley.recaf.services.mapping.matching;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;

import java.util.List;

/**
 * Class match between source and target versions.
 *
 * @param sourcePath
 * 		Path to the source class.
 * @param targetPath
 * 		Path to the matched target class.
 * @param similarity
 * 		Class similarity in range {@code [0.0, 1.0]}.
 * @param certaintyGap
 * 		Top-vs-runner-up similarity gap in range {@code [0.0, 1.0]}.
 * @param fieldMatches
 * 		Accepted field pairings.
 * @param methodMatches
 * 		Accepted method pairings.
 *
 * @author Matt Coley
 */
public record SimilarityClassMatch(@Nonnull ClassPathNode sourcePath,
                                   @Nonnull ClassPathNode targetPath,
                                   double similarity,
                                   double certaintyGap,
                                   @Nonnull List<SimilarityMemberMatch> fieldMatches,
                                   @Nonnull List<SimilarityMemberMatch> methodMatches) {}
