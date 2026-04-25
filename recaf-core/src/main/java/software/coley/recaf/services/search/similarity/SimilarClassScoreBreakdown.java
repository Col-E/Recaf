package software.coley.recaf.services.search.similarity;

/**
 * Structured class similarity score breakdown.
 *
 * @param methodSimilarity
 * 		Aggregated method similarity before penalties.
 * @param fieldSimilarity
 * 		Aggregated field similarity before penalties.
 * @param methodMismatchPenalty
 * 		Method count / compatibility penalty.
 * @param fieldMismatchPenalty
 * 		Field count / compatibility penalty.
 *
 * @author Matt Coley
 */
public record SimilarClassScoreBreakdown(double methodSimilarity,
                                         double fieldSimilarity,
                                         double methodMismatchPenalty,
                                         double fieldMismatchPenalty) {}
