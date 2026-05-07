package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.collections.box.IntBox;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.collect.primitive.Object2IntMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleBiFunction;

/**
 * Class similarity scoring utilities.
 *
 * @author Matt Coley
 * @see MethodSimilarityScorer
 */
public class ClassSimilarityScorer {
	// Weights for method scoring when both methods have code fingerprints. Sums to 1.0.
	private static final double METHOD_FP_WEIGHT_FINGERPRINT = 0.90;
	private static final double METHOD_FP_WEIGHT_MODIFIER = 0.10;

	// Weights for method scoring when one or both methods do not have code fingerprints. Sums to 1.0.
	private static final double METHOD_FALLBACK_WEIGHT_PARAMETER = 0.45;
	private static final double METHOD_FALLBACK_WEIGHT_RETURN = 0.35;
	private static final double METHOD_FALLBACK_WEIGHT_MODIFIER = 0.20;

	// Weights for field scoring. Sums to 1.0.
	private static final double FIELD_WEIGHT_DESCRIPTOR = 0.75;
	private static final double FIELD_WEIGHT_MODIFIER = 0.25;

	// Reduced score awarded when return types do not match exactly but collapse to the same normalized token.
	private static final double RETURN_FALLBACK_NORMALIZED_MATCH_SCORE = 0.85;

	// Penalty applied to method comparisons when one method is abstract or native and the other is not.
	private static final double ABS_NON_ABS_PENALTY = 0.15;

	// Minimum parameter and return similarity required to consider two methods compatible for permutation matching.
	// Tuned to allow some flexibility, but strict enough to reduce a significant number of clearly incompatible pairings.
	private static final double MIN_PERM_SCORE = 0.5;

	private ClassSimilarityScorer() {}

	/**
	 * @param classInfo
	 * 		Class to prepare for repeated comparisons.
	 *
	 * @return Comparison class model.
	 */
	@Nonnull
	public static ClassComparisonModel buildModel(@Nonnull ClassInfo classInfo) {
		MethodFingerprinting.Lookup lookup = MethodFingerprinting.lookupFor(classInfo);
		List<MethodComparisonModel> methods = classInfo.getMethods().stream()
				.map(method -> MethodComparisonModel.from(method, lookup.fingerprint(method)))
				.toList();
		List<FieldComparisonModel> fields = classInfo.getFields().stream()
				.map(FieldComparisonModel::from)
				.toList();
		return new ClassComparisonModel(classInfo, methods, fields);
	}

	/**
	 * @param reference
	 * 		Reference class comparison model.
	 * @param candidate
	 * 		Candidate class comparison model.
	 * @param options
	 * 		Search options.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph for descriptor comparisons.
	 *
	 * @return Structured scoring breakdown.
	 */
	@Nonnull
	public static SimilarClassScoreBreakdown breakdown(@Nonnull ClassComparisonModel reference,
	                                                   @Nonnull ClassComparisonModel candidate,
	                                                   @Nonnull SimilarClassSearchOptions options,
	                                                   @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                                   @Nonnull InheritanceGraph inheritanceGraph) {
		PairingScore methodScore = compareMethods(reference.methods(), candidate.methods(),
				options.methodDeclarationOrderMode(), options, methodSimilarityScorer, inheritanceGraph).toScore();
		PairingScore fieldScore = compareFields(reference.fields(), candidate.fields(), options.fieldDeclarationOrderMode()).toScore();
		return new SimilarClassScoreBreakdown(
				methodScore.similarity(),
				fieldScore.similarity(),
				methodScore.mismatchPenalty(),
				fieldScore.mismatchPenalty()
		);
	}

	/**
	 * @param reference
	 * 		Reference class comparison model.
	 * @param candidate
	 * 		Candidate class comparison model.
	 * @param options
	 * 		Search options.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph for descriptor comparisons.
	 *
	 * @return Structured scoring breakdown and selected member pairings.
	 */
	@Nonnull
	public static DetailedClassSimilarity detailedBreakdown(@Nonnull ClassComparisonModel reference,
	                                                        @Nonnull ClassComparisonModel candidate,
	                                                        @Nonnull SimilarClassSearchOptions options,
	                                                        @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                                        @Nonnull InheritanceGraph inheritanceGraph) {
		PairingResult<MethodComparisonModel> methodScore = compareMethods(reference.methods(), candidate.methods(),
				options.methodDeclarationOrderMode(), options, methodSimilarityScorer, inheritanceGraph);
		PairingResult<FieldComparisonModel> fieldScore = compareFields(reference.fields(), candidate.fields(),
				options.fieldDeclarationOrderMode());
		return new DetailedClassSimilarity(
				new SimilarClassScoreBreakdown(
						methodScore.similarity(),
						fieldScore.similarity(),
						methodScore.mismatchPenalty(),
						fieldScore.mismatchPenalty()
				),
				methodScore.matches().stream()
						.map(match -> new MethodSimilarityMatch(match.reference().method(), match.candidate().method(), match.similarity()))
						.toList(),
				fieldScore.matches().stream()
						.map(match -> new FieldSimilarityMatch(match.reference().field(), match.candidate().field(), match.similarity()))
						.toList()
		);
	}

	/**
	 * @param breakdown
	 * 		Score breakdown.
	 * @param options
	 * 		Search options.
	 *
	 * @return Overall class similarity score.
	 */
	public static double score(@Nonnull SimilarClassScoreBreakdown breakdown,
	                           @Nonnull SimilarClassSearchOptions options) {
		// Use caller-provided weights when possible, but fall back to the defaults if the caller
		// disables both dimensions by assigning them non-positive weights.
		double methodWeight = Math.max(options.methodWeight(), 0);
		double fieldWeight = Math.max(options.fieldWeight(), 0);
		double totalWeight = methodWeight + fieldWeight;
		if (totalWeight <= 0) {
			methodWeight = SimilarClassSearchOptions.DEFAULT_METHOD_WEIGHT;
			fieldWeight = SimilarClassSearchOptions.DEFAULT_FIELD_WEIGHT;
			totalWeight = methodWeight + fieldWeight;
		}

		double weightedSimilarity = ((breakdown.methodSimilarity() * methodWeight)
				+ (breakdown.fieldSimilarity() * fieldWeight)) / totalWeight;
		double weightedPenalty = ((breakdown.methodMismatchPenalty() * methodWeight)
				+ (breakdown.fieldMismatchPenalty() * fieldWeight)) / totalWeight;
		return normalize(weightedSimilarity - weightedPenalty);
	}

	/**
	 * Compare method members with the given ordering mode.
	 *
	 * @param reference
	 * 		Reference class methods.
	 * @param candidate
	 * 		Candidate class methods.
	 * @param orderMode
	 * 		Method declaration ordering mode.
	 * @param options
	 * 		Class similarity search options.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph used for descriptor compatibility checks.
	 *
	 * @return Method pairing score and mismatch penalty.
	 */
	@Nonnull
	private static PairingResult<MethodComparisonModel> compareMethods(@Nonnull List<MethodComparisonModel> reference,
	                                                                   @Nonnull List<MethodComparisonModel> candidate,
	                                                                   @Nonnull MemberOrderMode orderMode,
	                                                                   @Nonnull SimilarClassSearchOptions options,
	                                                                   @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                                                   @Nonnull InheritanceGraph inheritanceGraph) {
		return switch (orderMode) {
			case ORDERED -> compareOrdered(reference, candidate, (left, right) ->
					methodSimilarity(left, right, options, methodSimilarityScorer, inheritanceGraph));
			case PERMUTATION -> compareUnordered(reference, candidate,
					(left, right) -> methodSimilarity(left, right, options, methodSimilarityScorer, inheritanceGraph),
					(left, right) -> permutationCompatible(left, right, options, methodSimilarityScorer, inheritanceGraph));
			case IGNORE_ORDER -> compareUnordered(reference, candidate,
					(left, right) -> methodSimilarity(left, right, options, methodSimilarityScorer, inheritanceGraph),
					(left, right) -> true);
		};
	}

	/**
	 * Compare field members with the given ordering mode.
	 *
	 * @param reference
	 * 		Reference class fields.
	 * @param candidate
	 * 		Candidate class fields.
	 * @param orderMode
	 * 		Field declaration ordering mode.
	 *
	 * @return Field pairing score and mismatch penalty.
	 */
	@Nonnull
	private static PairingResult<FieldComparisonModel> compareFields(@Nonnull List<FieldComparisonModel> reference,
	                                                                 @Nonnull List<FieldComparisonModel> candidate,
	                                                                 @Nonnull MemberOrderMode orderMode) {
		return switch (orderMode) {
			case ORDERED -> compareOrdered(reference, candidate, ClassSimilarityScorer::fieldSimilarity);
			case PERMUTATION -> compareUnordered(reference, candidate,
					ClassSimilarityScorer::fieldSimilarity,
					ClassSimilarityScorer::permutationCompatible);
			case IGNORE_ORDER -> compareUnordered(reference, candidate,
					ClassSimilarityScorer::fieldSimilarity,
					(left, right) -> true);
		};
	}

	/**
	 * Compare two ordered member lists by index.
	 *
	 * @param reference
	 * 		Reference members.
	 * @param candidate
	 * 		Candidate members.
	 * @param scorer
	 * 		Per-member similarity scorer.
	 * @param <T>
	 * 		Member model type.
	 *
	 * @return Aggregate similarity and mismatch penalty for the ordered lists.
	 */
	@Nonnull
	private static <T> PairingResult<T> compareOrdered(@Nonnull List<T> reference,
	                                                   @Nonnull List<T> candidate,
	                                                   @Nonnull ToDoubleBiFunction<T, T> scorer) {
		// If both are empty, it's a perfect match.
		if (reference.isEmpty() && candidate.isEmpty())
			return new PairingResult<>(1, 0, List.of());

		// When one is empty and the other is not, there are no matches and a full penalty.
		int matched = Math.min(reference.size(), candidate.size());
		if (matched == 0)
			return new PairingResult<>(0, 1, List.of());

		// Compare only overlapping indexes.
		double total = 0;
		List<PairingMatch<T>> matches = new ArrayList<>(matched);
		for (int i = 0; i < matched; i++) {
			T left = reference.get(i);
			T right = candidate.get(i);
			double similarity = scorer.applyAsDouble(left, right);
			total += similarity;
			matches.add(new PairingMatch<>(left, right, similarity));
		}
		double similarity = normalize(total / matched);

		// Penalize any unmatched members.
		double diff = Math.abs(reference.size() - candidate.size());
		double max = Math.max(Math.max(reference.size(), candidate.size()), 1);
		double mismatchPenalty = normalize(diff / max);
		return new PairingResult<>(similarity, mismatchPenalty, List.copyOf(matches));
	}

	/**
	 * Compare two unordered member lists by greedily selecting the best remaining compatible pairings.
	 *
	 * @param reference
	 * 		Reference members.
	 * @param candidate
	 * 		Candidate members.
	 * @param scorer
	 * 		Per-member similarity scorer.
	 * @param compatibility
	 * 		Predicate used to reject impossible pairings before scoring them.
	 * @param <T>
	 * 		Member model type.
	 *
	 * @return Aggregate similarity and mismatch penalty for the unordered lists.
	 */
	@Nonnull
	private static <T> PairingResult<T> compareUnordered(@Nonnull List<T> reference,
	                                                     @Nonnull List<T> candidate,
	                                                     @Nonnull ToDoubleBiFunction<T, T> scorer,
	                                                     @Nonnull BiPredicate<T, T> compatibility) {
		// Handle empty edge cases, same as above.
		if (reference.isEmpty() && candidate.isEmpty())
			return new PairingResult<>(1, 0, List.of());
		if (reference.isEmpty() || candidate.isEmpty())
			return new PairingResult<>(0, 1, List.of());

		// Build list of all compatible pairs with their similarity scores so we can consume the best ones first.
		List<Pair> pairs = new ArrayList<>();
		for (int i = 0; i < reference.size(); i++) {
			T left = reference.get(i);
			for (int j = 0; j < candidate.size(); j++) {
				T right = candidate.get(j);
				if (!compatibility.test(left, right))
					continue;
				double similarity = scorer.applyAsDouble(left, right);
				if (similarity > 0)
					pairs.add(new Pair(i, j, similarity));
			}
		}

		// Greedily consume the highest-scoring edges first so each member participates in at most one pair.
		pairs.sort(Comparator.comparingDouble(Pair::similarity).reversed());
		boolean[] usedReference = new boolean[reference.size()];
		boolean[] usedCandidate = new boolean[candidate.size()];
		double total = 0;
		int matched = 0;
		List<PairingMatch<T>> matches = new ArrayList<>();
		for (Pair pair : pairs) {
			if (usedReference[pair.referenceIndex()] || usedCandidate[pair.candidateIndex()])
				continue;
			usedReference[pair.referenceIndex()] = true;
			usedCandidate[pair.candidateIndex()] = true;
			total += pair.similarity();
			matched++;
			matches.add(new PairingMatch<>(reference.get(pair.referenceIndex()), candidate.get(pair.candidateIndex()), pair.similarity()));
		}

		if (matched == 0)
			return new PairingResult<>(0, 1, List.of());

		// Penalize any unmatched members.
		double unmatched = countUnused(usedReference) + countUnused(usedCandidate);
		double max = Math.max(Math.max(reference.size(), candidate.size()), 1);
		double similarity = normalize(total / matched);
		double mismatchPenalty = normalize(unmatched / max);
		return new PairingResult<>(similarity, mismatchPenalty, List.copyOf(matches));
	}

	private static int countUnused(boolean[] usage) {
		IntBox count = new IntBox(0);
		for (boolean used : usage)
			if (!used)
				count.increment();
		return count.get();
	}

	/**
	 * @param reference
	 * 		Reference method comparison model.
	 * @param candidate
	 * 		Candidate method comparison model.
	 * @param options
	 * 		Class search options controlling descriptor compatibility.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph used for relaxed return-type compatibility checks.
	 *
	 * @return {@code true} when the two methods are compatible enough to be considered in
	 * permutation matching.
	 *
	 * @implNote This is stricter than plain scoring because permutation mode is intended
	 * to match equivalent declared members regardless of declaration order, not arbitrary best fits.
	 */
	private static boolean permutationCompatible(@Nonnull MethodComparisonModel reference,
	                                             @Nonnull MethodComparisonModel candidate,
	                                             @Nonnull SimilarClassSearchOptions options,
	                                             @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                             @Nonnull InheritanceGraph inheritanceGraph) {
		if (reference.kind() != candidate.kind())
			return false;
		return parameterShapeScore(reference.parameterTypes(), candidate.parameterTypes(), options.parameterMatchMode()) > MIN_PERM_SCORE
				&& returnShapeScore(reference.returnType(), candidate.returnType(), options.returnMatchMode(), methodSimilarityScorer, inheritanceGraph) > MIN_PERM_SCORE;
	}

	/**
	 * @param reference
	 * 		Reference field comparison model.
	 * @param candidate
	 * 		Candidate field comparison model.
	 *
	 * @return {@code true} when the two fields are compatible enough to be treated as a declaration
	 * permutation of one another. Field names are intentionally ignored.
	 */
	private static boolean permutationCompatible(@Nonnull FieldComparisonModel reference, @Nonnull FieldComparisonModel candidate) {
		return reference.normalizedDescriptor().equals(candidate.normalizedDescriptor())
				&& reference.modifierMask() == candidate.modifierMask();
	}

	/**
	 * Compute similarity between two method models.
	 *
	 * @param reference
	 * 		Reference method model.
	 * @param candidate
	 * 		Candidate method model.
	 * @param options
	 * 		Class similarity search options.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph used for relaxed return-type compatibility and scoring.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double methodSimilarity(@Nonnull MethodComparisonModel reference,
	                                       @Nonnull MethodComparisonModel candidate,
	                                       @Nonnull SimilarClassSearchOptions options,
	                                       @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                       @Nonnull InheritanceGraph inheritanceGraph) {
		// Skip obviously incompatible members to save time on expensive fingerprint comparisons.
		if (reference.kind() != candidate.kind())
			return 0;
		double parameterScore = parameterShapeScore(reference.parameterTypes(), candidate.parameterTypes(), options.parameterMatchMode());
		double returnScore = returnShapeScore(reference.returnType(), candidate.returnType(), options.returnMatchMode(), methodSimilarityScorer, inheritanceGraph);
		if (parameterScore <= 0 || returnScore <= 0)
			return 0;

		// Prefer executable similarity when both methods contain enough information to fingerprint.
		double modifierScore = modifierSimilarity(reference.modifierMask(), candidate.modifierMask(),
				MethodComparisonModel.MODIFIER_FEATURES);
		if (reference.fingerprint() != null && candidate.fingerprint() != null) {
			double fingerprintScore = methodSimilarityScorer.score(reference.fingerprint(), candidate.fingerprint(), inheritanceGraph);
			return normalize((fingerprintScore * METHOD_FP_WEIGHT_FINGERPRINT)
					+ (modifierScore * METHOD_FP_WEIGHT_MODIFIER));
		}

		// Fall back to descriptor shape and modifiers when at least one method is abstract/native.
		double score = ((parameterScore * METHOD_FALLBACK_WEIGHT_PARAMETER)
				+ (returnScore * METHOD_FALLBACK_WEIGHT_RETURN)
				+ (modifierScore * METHOD_FALLBACK_WEIGHT_MODIFIER)) * ABS_NON_ABS_PENALTY;
		return normalize(score);
	}

	/**
	 * Compute similarity between two field models.
	 *
	 * @param reference
	 * 		Reference field model.
	 * @param candidate
	 * 		Candidate field model.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double fieldSimilarity(@Nonnull FieldComparisonModel reference, @Nonnull FieldComparisonModel candidate) {
		double descriptorScore = reference.normalizedDescriptor().equals(candidate.normalizedDescriptor()) ? 1 : 0;
		double modifierScore = modifierSimilarity(reference.modifierMask(), candidate.modifierMask(),
				FieldComparisonModel.MODIFIER_FEATURES);
		return normalize((descriptorScore * FIELD_WEIGHT_DESCRIPTOR) + (modifierScore * FIELD_WEIGHT_MODIFIER));
	}

	/**
	 * Score parameter similarity for non-fingerprinted method comparison.
	 *
	 * @param reference
	 * 		Reference parameter types.
	 * @param candidate
	 * 		Candidate parameter types.
	 * @param mode
	 * 		Configured parameter matching mode.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double parameterShapeScore(@Nonnull Type[] reference,
	                                          @Nonnull Type[] candidate,
	                                          @Nonnull ParameterMatchMode mode) {
		if (mode == ParameterMatchMode.ANYTHING)
			return 1;

		// Normalize to sort-based tokens so unrelated owner/package names do not dominate fallback scoring.
		List<String> referenceTokens = normalizeTypes(reference);
		List<String> candidateTokens = normalizeTypes(candidate);

		// Score based on the fraction of matching parameter types.
		int maxCount = Math.max(Math.max(referenceTokens.size(), candidateTokens.size()), 1);
		return switch (mode) {
			case EXACT_COUNT_AND_ORDER -> {
				// Straight forward, check each parameter at each index for exact token matches.
				int orderedMatches = 0;
				for (int i = 0; i < Math.min(referenceTokens.size(), candidateTokens.size()); i++)
					if (referenceTokens.get(i).equals(candidateTokens.get(i)))
						orderedMatches++;
				yield referenceTokens.isEmpty() && candidateTokens.isEmpty() ? 1 : orderedMatches / (double) maxCount;
			}
			case EXACT_COUNT_ANY_ORDER -> {
				// Bag the tokens then count the intersections.
				Object2IntMap<String> left = MethodInstructionNormalizer.multiset(referenceTokens);
				Object2IntMap<String> right = MethodInstructionNormalizer.multiset(candidateTokens);
				IntBox intersection = new IntBox(0);
				left.forEach((key, value) -> intersection.increment(Math.min(value, right.getOrDefault(key, 0))));
				yield referenceTokens.isEmpty() && candidateTokens.isEmpty() ? 1 : intersection.get() / (double) maxCount;
			}
			default -> throw new IllegalStateException("Unexpected value: " + mode);
		};
	}

	/**
	 * Score return-type similarity for non-fingerprinted method comparison.
	 *
	 * @param reference
	 * 		Reference return type.
	 * @param candidate
	 * 		Candidate return type.
	 * @param mode
	 * 		Configured return matching mode.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph used for assignability checks.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double returnShapeScore(@Nonnull Type reference,
	                                       @Nonnull Type candidate,
	                                       @Nonnull ReturnMatchMode mode,
	                                       @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                       @Nonnull InheritanceGraph inheritanceGraph) {
		if (reference.equals(candidate))
			return 1;
		if (mode == ReturnMatchMode.ANY_ASSIGNABLE
				&& methodSimilarityScorer.areTypesAssignableEitherDirection(reference, candidate, inheritanceGraph))
			return 1;
		return normalizeType(reference).equals(normalizeType(candidate)) ? RETURN_FALLBACK_NORMALIZED_MATCH_SCORE : 0;
	}

	/**
	 * Compare normalized modifier bitmasks feature-by-feature.
	 *
	 * @param referenceMask
	 * 		Reference modifier mask.
	 * @param candidateMask
	 * 		Candidate modifier mask.
	 * @param features
	 * 		Feature bits to compare.
	 *
	 * @return Fraction of matching modifier features, {@code [0.0, 1.0]}.
	 */
	private static double modifierSimilarity(int referenceMask, int candidateMask, int[] features) {
		double matches = 0;
		for (int feature : features)
			if ((referenceMask & feature) == (candidateMask & feature))
				matches++;
		return features.length == 0 ? 1 : matches / features.length;
	}

	/**
	 * @param types
	 * 		Array of types to normalize.
	 *
	 * @return List of simplified type tokens, preserving encounter order.
	 */
	@Nonnull
	private static List<String> normalizeTypes(@Nonnull Type[] types) {
		List<String> normalized = new ArrayList<>(types.length);
		for (Type type : types)
			normalized.add(normalizeType(type));
		return normalized;
	}

	/**
	 * Collapse a type down to a coarse token based on {@link Type#getSort() sort} and array depth.
	 * <p>
	 * This intentionally ignores object type names so comparisons stay resilient when
	 * matching obfuscated classes against their unobfuscated counterparts.
	 *
	 * @param type
	 * 		Type to normalize.
	 *
	 * @return Simplified descriptor token.
	 */
	@Nonnull
	private static String normalizeType(@Nonnull Type type) {
		int sort = type.getSort();
		if (sort == Type.ARRAY)
			return "[".repeat(type.getDimensions()) + Types.getSortName(type.getElementType().getSort());
		return Types.getSortName(sort);
	}

	/**
	 * @param value
	 * 		Value to normalize.
	 *
	 * @return Value clamped to {@code [0.0, 1.0]}.
	 */
	private static double normalize(double value) {
		return Math.clamp(value, 0, 1);
	}

	/**
	 * Normalize the field modifier set into a compact feature bitmask that ignores unrelated flags
	 * and preserves package-private visibility as an explicit state.
	 *
	 * @param field
	 * 		Field to inspect.
	 *
	 * @return Comparison-oriented modifier bitmask.
	 */
	private static int normalizeFieldModifiers(@Nonnull FieldMember field) {
		int mask = normalizeVisibility(field.hasPublicModifier(), field.hasProtectedModifier(), field.hasPrivateModifier(),
				FieldComparisonModel.VISIBILITY_PUBLIC, FieldComparisonModel.VISIBILITY_PROTECTED,
				FieldComparisonModel.VISIBILITY_PRIVATE, FieldComparisonModel.VISIBILITY_PACKAGE);
		if (field.hasStaticModifier()) mask |= FieldComparisonModel.STATIC;
		if (field.hasFinalModifier()) mask |= FieldComparisonModel.FINAL;
		if (field.hasVolatileModifier()) mask |= FieldComparisonModel.VOLATILE;
		if (field.hasTransientModifier()) mask |= FieldComparisonModel.TRANSIENT;
		if (field.hasEnumModifier()) mask |= FieldComparisonModel.ENUM;
		if (field.hasSyntheticModifier()) mask |= FieldComparisonModel.SYNTHETIC;
		return mask;
	}

	/**
	 * Normalize visibility flags into a single mutually-exclusive feature bit.
	 *
	 * @param isPublic
	 * 		Whether the member is {@code public}.
	 * @param isProtected
	 * 		Whether the member is {@code protected}.
	 * @param isPrivate
	 * 		Whether the member is {@code private}.
	 * @param publicMask
	 * 		Mask to use for {@code public}.
	 * @param protectedMask
	 * 		Mask to use for {@code protected}.
	 * @param privateMask
	 * 		Mask to use for {@code private}.
	 * @param packageMask
	 * 		Mask to use for package-private visibility.
	 *
	 * @return One of the given visibility masks.
	 */
	private static int normalizeVisibility(boolean isPublic,
	                                       boolean isProtected,
	                                       boolean isPrivate,
	                                       int publicMask,
	                                       int protectedMask,
	                                       int privateMask,
	                                       int packageMask) {
		if (isPublic) return publicMask;
		if (isProtected) return protectedMask;
		if (isPrivate) return privateMask;
		return packageMask;
	}

	/**
	 * Pairing summary containing the aggregate similarity for the matched pairs.
	 *
	 * @param similarity
	 * 		Aggregate similarity of the matched pairs.
	 * @param mismatchPenalty
	 * 		Penalty for unmatched members.
	 */
	private record PairingScore(double similarity, double mismatchPenalty) {}

	/**
	 * Aggregate result of member pairing with the selected matches and their similarity scores.
	 *
	 * @param similarity
	 * 		Aggregate similarity of the matched pairs.
	 * @param mismatchPenalty
	 * 		Penalty for unmatched members.
	 * @param matches
	 * 		List of selected member pairings with their individual similarity scores.
	 * @param <T>
	 * 		Type of the paired member.
	 */
	private record PairingResult<T>(double similarity,
	                                double mismatchPenalty,
	                                @Nonnull List<PairingMatch<T>> matches) {
		@Nonnull
		private PairingScore toScore() {
			return new PairingScore(similarity, mismatchPenalty);
		}
	}

	/**
	 * Matched pair of members with their similarity score.
	 *
	 * @param reference
	 * 		Source member.
	 * @param candidate
	 * 		Target member.
	 * @param similarity
	 * 		Similarity score for the pair, in {@code [0.0, 1.0]}.
	 * @param <T>
	 * 		Type of the paired member.
	 */
	private record PairingMatch<T>(@Nonnull T reference,
	                               @Nonnull T candidate,
	                               double similarity) {}

	/**
	 * Score-bearing pair of member indexes used for greedy matching of unordered members.
	 *
	 * @param referenceIndex
	 * 		Index of the reference member in its list.
	 * @param candidateIndex
	 * 		Index of the candidate member in its list.
	 * @param similarity
	 * 		Similarity score for the pair, used to prioritize better matches in unordered comparison modes.
	 *
	 * @see #compareUnordered(List, List, ToDoubleBiFunction, BiPredicate)
	 */
	private record Pair(int referenceIndex, int candidateIndex, double similarity) {}

	/**
	 * Comparisons model for class comparisons.
	 *
	 * @param classInfo
	 * 		Wrapped class.
	 * @param methods
	 * 		Comparison models for declared methods.
	 * @param fields
	 * 		Comparison models for declared fields.
	 */
	public record ClassComparisonModel(@Nonnull ClassInfo classInfo,
	                                   @Nonnull List<MethodComparisonModel> methods,
	                                   @Nonnull List<FieldComparisonModel> fields) {}

	/**
	 * Comparison model for methods.
	 *
	 * @param method
	 * 		Wrapped method.
	 * @param fingerprint
	 * 		Executable fingerprint, when available.
	 * @param parameterTypes
	 * 		Method parameter types.
	 * @param returnType
	 * 		Method return type.
	 * @param modifierMask
	 * 		Normalized modifier-feature mask used for similarity comparison.
	 * @param kind
	 * 		Method kind bucket used to keep constructors and class initializers separate from regular methods.
	 */
	public record MethodComparisonModel(@Nonnull MethodMember method,
	                                    @Nullable MethodFingerprint fingerprint,
	                                    @Nonnull Type[] parameterTypes,
	                                    @Nonnull Type returnType,
	                                    int modifierMask,
	                                    @Nonnull MethodKind kind) {
		private static final int VISIBILITY_PUBLIC = 1;
		private static final int VISIBILITY_PROTECTED = 1 << 1;
		private static final int VISIBILITY_PRIVATE = 1 << 2;
		private static final int VISIBILITY_PACKAGE = 1 << 3;
		private static final int STATIC = 1 << 4;
		private static final int FINAL = 1 << 5;
		private static final int SYNCHRONIZED = 1 << 6;
		private static final int NATIVE = 1 << 7;
		private static final int ABSTRACT = 1 << 8;
		private static final int BRIDGE = 1 << 9;
		private static final int SYNTHETIC = 1 << 10;
		private static final int VARARGS = 1 << 11;
		private static final int[] MODIFIER_FEATURES = {
				VISIBILITY_PUBLIC, VISIBILITY_PROTECTED, VISIBILITY_PRIVATE, VISIBILITY_PACKAGE,
				STATIC, FINAL, SYNCHRONIZED, NATIVE, ABSTRACT, BRIDGE, SYNTHETIC, VARARGS
		};

		/**
		 * Build a comparison-ready method model from the raw member.
		 *
		 * @param method
		 * 		Method to wrap.
		 * @param fingerprint
		 * 		Precomputed fingerprint, if available.
		 *
		 * @return Comparison-ready method model.
		 */
		@Nonnull
		private static MethodComparisonModel from(@Nonnull MethodMember method, @Nullable MethodFingerprint fingerprint) {
			Type methodType = Type.getMethodType(method.getDescriptor());
			return new MethodComparisonModel(
					method,
					fingerprint,
					methodType.getArgumentTypes(),
					methodType.getReturnType(),
					normalizeMethodModifiers(method),
					MethodKind.from(method)
			);
		}

		/**
		 * Normalize the method modifier set into a compact feature bitmask that ignores unrelated flags
		 * and preserves package-private visibility as an explicit state.
		 *
		 * @param method
		 * 		Method to inspect.
		 *
		 * @return Comparison-oriented modifier bitmask.
		 */
		private static int normalizeMethodModifiers(@Nonnull MethodMember method) {
			int mask = normalizeVisibility(method.hasPublicModifier(), method.hasProtectedModifier(), method.hasPrivateModifier(),
					MethodComparisonModel.VISIBILITY_PUBLIC, MethodComparisonModel.VISIBILITY_PROTECTED,
					MethodComparisonModel.VISIBILITY_PRIVATE, MethodComparisonModel.VISIBILITY_PACKAGE);
			if (method.hasStaticModifier()) mask |= MethodComparisonModel.STATIC;
			if (method.hasFinalModifier()) mask |= MethodComparisonModel.FINAL;
			if (method.hasSynchronizedModifier()) mask |= MethodComparisonModel.SYNCHRONIZED;
			if (method.hasNativeModifier()) mask |= MethodComparisonModel.NATIVE;
			if (method.hasAbstractModifier()) mask |= MethodComparisonModel.ABSTRACT;
			if (method.hasBridgeModifier()) mask |= MethodComparisonModel.BRIDGE;
			if (method.hasSyntheticModifier()) mask |= MethodComparisonModel.SYNTHETIC;
			if (method.hasVarargsModifier()) mask |= MethodComparisonModel.VARARGS;
			return mask;
		}
	}

	/**
	 * Comparison model for fields.
	 *
	 * @param field
	 * 		Wrapped field.
	 * @param normalizedDescriptor
	 * 		Normalized type descriptor token with owner names removed.
	 * @param modifierMask
	 * 		Normalized modifier-feature mask used for similarity comparison.
	 */
	public record FieldComparisonModel(@Nonnull FieldMember field,
	                                   @Nonnull String normalizedDescriptor,
	                                   int modifierMask) {
		private static final int VISIBILITY_PUBLIC = 1;
		private static final int VISIBILITY_PROTECTED = 1 << 1;
		private static final int VISIBILITY_PRIVATE = 1 << 2;
		private static final int VISIBILITY_PACKAGE = 1 << 3;
		private static final int STATIC = 1 << 4;
		private static final int FINAL = 1 << 5;
		private static final int VOLATILE = 1 << 6;
		private static final int TRANSIENT = 1 << 7;
		private static final int ENUM = 1 << 8;
		private static final int SYNTHETIC = 1 << 9;
		private static final int[] MODIFIER_FEATURES = {
				VISIBILITY_PUBLIC, VISIBILITY_PROTECTED, VISIBILITY_PRIVATE, VISIBILITY_PACKAGE,
				STATIC, FINAL, VOLATILE, TRANSIENT, ENUM, SYNTHETIC
		};

		/**
		 * Build a comparison-ready field model from the raw member.
		 *
		 * @param field
		 * 		Field to wrap.
		 *
		 * @return Comparison-ready field model.
		 */
		@Nonnull
		private static FieldComparisonModel from(@Nonnull FieldMember field) {
			return new FieldComparisonModel(field, normalizeType(Type.getType(field.getDescriptor())), normalizeFieldModifiers(field));
		}
	}

	/**
	 * Detailed class similarity result including selected member pairings.
	 *
	 * @param breakdown
	 * 		Structured score breakdown.
	 * @param methodMatches
	 * 		Selected method pairings.
	 * @param fieldMatches
	 * 		Selected field pairings.
	 */
	public record DetailedClassSimilarity(@Nonnull SimilarClassScoreBreakdown breakdown,
	                                      @Nonnull List<MethodSimilarityMatch> methodMatches,
	                                      @Nonnull List<FieldSimilarityMatch> fieldMatches) {}

	/**
	 * Pairing of similar methods.
	 *
	 * @param reference
	 * 		Reference method.
	 * @param candidate
	 * 		Candidate method.
	 * @param similarity
	 * 		Pairing similarity.
	 */
	public record MethodSimilarityMatch(@Nonnull MethodMember reference,
	                                    @Nonnull MethodMember candidate,
	                                    double similarity) {}

	/**
	 * Pairing of similar fields.
	 *
	 * @param reference
	 * 		Reference field.
	 * @param candidate
	 * 		Candidate field.
	 * @param similarity
	 * 		Pairing similarity.
	 */
	public record FieldSimilarityMatch(@Nonnull FieldMember reference,
	                                   @Nonnull FieldMember candidate,
	                                   double similarity) {}

	/**
	 * Method kind bucket used to avoid pairing constructors and class initializers with normal methods.
	 */
	public enum MethodKind {
		CONSTRUCTOR,
		CLASS_INITIALIZER,
		REGULAR;

		/**
		 * @param method
		 * 		Method to categorize.
		 *
		 * @return Method kind bucket based on the special JVM method names.
		 */
		@Nonnull
		public static MethodKind from(@Nonnull MethodMember method) {
			return switch (method.getName()) {
				case "<init>" -> CONSTRUCTOR;
				case "<clinit>" -> CLASS_INITIALIZER;
				default -> REGULAR;
			};
		}
	}
}
