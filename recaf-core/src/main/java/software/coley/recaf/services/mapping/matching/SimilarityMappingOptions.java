package software.coley.recaf.services.mapping.matching;

/**
 * Options for similarity-based mapping generation.
 *
 * @param classSimilarityThresholdPercent
 * 		Minimum class similarity threshold in percent, {@code [0, 100]}.
 * @param classCertaintyGapPercent
 * 		Minimum top-vs-runner-up class similarity gap in percent, {@code [0, 100]}.
 * @param memberSimilarityThresholdPercent
 * 		Minimum member similarity threshold in percent, {@code [0, 100]}.
 * @param maxFullScoreCandidates
 * 		Maximum number of candidates retained for scoring after shortlist pre-screening.
 * 	    More candidates means more work to do, but also means more chances to find a good match.
 * @param shortlistGapThresholdPercent
 * 		Structural shortlist score gap threshold used to aggressively drop much-worse candidates, {@code [0, 100]}.
 * 		A new candidate with this much better score triggers all prior candidates with worse scores to be dropped.
 *
 * @author Matt Coley
 */
public record SimilarityMappingOptions(int classSimilarityThresholdPercent,
                                       int classCertaintyGapPercent,
                                       int memberSimilarityThresholdPercent,
                                       int maxFullScoreCandidates,
                                       int shortlistGapThresholdPercent) {}
