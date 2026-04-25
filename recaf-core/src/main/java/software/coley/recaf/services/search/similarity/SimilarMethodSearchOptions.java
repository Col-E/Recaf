package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.search.SimilaritySearchService;

/**
 * Options for {@link SimilaritySearchService}.
 *
 * @param similarityThresholdPercent
 * 		Minimum similarity threshold in percent.
 * @param parameterMatchMode
 * 		Parameter pre-filter mode.
 * @param returnMatchMode
 * 		Return type pre-filter mode.
 * @param primaryResourceOnly
 * 		Flag to only search the primary resource.
 *
 * @author Matt Coley
 */
public record SimilarMethodSearchOptions(int similarityThresholdPercent,
                                         @Nonnull ParameterMatchMode parameterMatchMode,
                                         @Nonnull ReturnMatchMode returnMatchMode,
                                         boolean primaryResourceOnly) {}
