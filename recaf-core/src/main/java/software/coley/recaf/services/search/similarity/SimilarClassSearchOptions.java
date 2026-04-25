package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.search.SimilaritySearchService;

/**
 * Options for {@link SimilaritySearchService} class searches.
 *
 * @param similarityThresholdPercent
 * 		Minimum similarity threshold in percent.
 * @param parameterMatchMode
 * 		Method parameter matching mode.
 * @param returnMatchMode
 * 		Method return matching mode.
 * @param methodDeclarationOrderMode
 * 		Method declaration ordering mode.
 * @param fieldDeclarationOrderMode
 * 		Field declaration ordering mode.
 * @param methodWeight
 * 		Method contribution weight.
 * @param fieldWeight
 * 		Field contribution weight.
 * @param searchScope
 * 		Candidate resource scope.
 *
 * @author Matt Coley
 */
public record SimilarClassSearchOptions(int similarityThresholdPercent,
                                        @Nonnull ParameterMatchMode parameterMatchMode,
                                        @Nonnull ReturnMatchMode returnMatchMode,
                                        @Nonnull MemberOrderMode methodDeclarationOrderMode,
                                        @Nonnull MemberOrderMode fieldDeclarationOrderMode,
                                        double methodWeight,
                                        double fieldWeight,
                                        @Nonnull SimilarClassSearchScope searchScope) {
	/** Default method weight. */
	public static final double DEFAULT_METHOD_WEIGHT = 0.70D;
	/** Default field weight. */
	public static final double DEFAULT_FIELD_WEIGHT = 0.30D;
}
