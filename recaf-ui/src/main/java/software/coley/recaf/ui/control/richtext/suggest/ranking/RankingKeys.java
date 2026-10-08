package software.coley.recaf.ui.control.richtext.suggest.ranking;

import jakarta.annotation.Nonnull;

import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Language-specific accessors used by {@link CompletionRanker}.
 *
 * @param rank
 * 		Structural rank. Lower values sort first.
 * @param sortKey
 * 		Primary text used for ordering ties.
 * @param displayText
 * 		Secondary text used for ordering ties.
 * @param learningKey
 * 		Stable identity used to record and look up learned usage.
 * 		When {@code null}, the completion is not learned.
 * @param baselineBoost
 * 		Raw built-in boost for the completion. The ranker caps it.
 * @param <T>
 * 		Completion type.
 *
 * @author Matt Coley
 */
public record RankingKeys<T>(@Nonnull ToIntFunction<T> rank,
                             @Nonnull Function<T, String> sortKey,
                             @Nonnull Function<T, String> displayText,
                             @Nonnull Function<T, String> learningKey,
                             @Nonnull ToIntFunction<T> baselineBoost) {}
