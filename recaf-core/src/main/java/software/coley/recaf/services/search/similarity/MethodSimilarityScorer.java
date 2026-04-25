package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.collections.box.IntBox;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.collect.primitive.Object2IntMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Similarity scoring utilities for method fingerprints.
 *
 * @author Matt Coley
 * @see ClassSimilarityScorer
 */
public final class MethodSimilarityScorer {
	private static final double RETURN_ASSIGNABLE_SCORE = 0.85D;

	// Weights for overall scoring of method similarity. Sums to 1.0.
	private static final double WEIGHT_INSN = 0.50D; // TODO: We should move these to 'SimilarMethodSearchOptions'
	private static final double WEIGHT_CFG = 0.25D;  //  to let users adjust values based on their needs.
	private static final double WEIGHT_DESC = 0.15D; //   - Still would need to validate summing to 1.
	private static final double WEIGHT_EX = 0.10D;   //   - If an item is 0, should also skip computing that portion.

	private MethodSimilarityScorer() {}

	/**
	 * @param reference
	 * 		Fingerprint of the reference method.
	 * @param candidateMethod
	 * 		Candidate method member.
	 * @param options
	 * 		Search options containing pre-filter modes.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return {@code true} if the candidate method passes the parameter
	 * and return type pre-filters based on the reference method and search options.
	 */
	public static boolean passesPrefilters(@Nonnull MethodFingerprint reference,
	                                       @Nonnull MethodMember candidateMethod,
	                                       @Nonnull SimilarMethodSearchOptions options,
	                                       @Nonnull InheritanceGraph inheritanceGraph) {
		Type[] candidateParameters = Type.getArgumentTypes(candidateMethod.getDescriptor());
		Type candidateReturnType = Type.getReturnType(candidateMethod.getDescriptor());

		if (!matchesParameters(reference.parameterTypes(), candidateParameters, options.parameterMatchMode()))
			return false;
		return matchesReturnPrefilter(reference.returnType(), candidateReturnType, options.returnMatchMode(), inheritanceGraph);
	}

	/**
	 * Compute similarity between the two given methods by their fingerprints.
	 *
	 * @param reference
	 * 		Method fingerprint of the reference method.
	 * @param candidate
	 * 		Method fingerprint of the candidate method.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	public static double score(@Nonnull MethodFingerprint reference,
	                           @Nonnull MethodFingerprint candidate,
	                           @Nonnull InheritanceGraph inheritanceGraph) {
		double instruction = sorensenDice(reference.trigrams(), candidate.trigrams());
		double cfg = compareVectors(reference.controlFlowVector(), candidate.controlFlowVector());
		double descriptor = (parameterScore(reference.parameterTypes(), candidate.parameterTypes())
				+ returnScore(reference.returnType(), candidate.returnType(), inheritanceGraph)) / 2D;
		double thrown = jaccard(reference.thrownTypes(), candidate.thrownTypes());
		return (WEIGHT_INSN * instruction)
				+ (WEIGHT_CFG * cfg)
				+ (WEIGHT_DESC * descriptor)
				+ (WEIGHT_EX * thrown);
	}

	/**
	 * @param referenceParameters
	 * 		Parameter types of the reference method.
	 * @param candidateParameters
	 * 		Parameter types of the candidate method.
	 * @param mode
	 * 		Parameter pre-filter mode.
	 *
	 * @return {@code true} if the candidate method matches the reference method's parameters
	 * according to the specified pre-filter mode, {@code false} otherwise.
	 */
	static boolean matchesParameters(@Nonnull Type[] referenceParameters,
	                                 @Nonnull Type[] candidateParameters,
	                                 @Nonnull ParameterMatchMode mode) {
		if (mode == ParameterMatchMode.ANYTHING)
			return true;

		List<String> referenceDescriptors = descriptors(referenceParameters);
		List<String> candidateDescriptors = descriptors(candidateParameters);
		return switch (mode) {
			case EXACT_COUNT_AND_ORDER -> referenceDescriptors.equals(candidateDescriptors);
			case EXACT_COUNT_ANY_ORDER -> MethodInstructionNormalizer.multiset(referenceDescriptors)
					.equals(MethodInstructionNormalizer.multiset(candidateDescriptors));
			default -> throw new IllegalStateException("Unexpected value: " + mode);
		};
	}

	/**
	 * @param referenceReturnType
	 * 		Return type of the reference method.
	 * @param candidateReturnType
	 * 		Return type of the candidate method.
	 * @param mode
	 * 		Return type pre-filter mode.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return {@code true} if the candidate method matches the reference method's return type according to the specified pre-filter mode, {@code false} otherwise.
	 */
	static boolean matchesReturnPrefilter(@Nonnull Type referenceReturnType,
	                                      @Nonnull Type candidateReturnType,
	                                      @Nonnull ReturnMatchMode mode,
	                                      @Nonnull InheritanceGraph inheritanceGraph) {
		return switch (mode) {
			case EXACT_TYPE -> referenceReturnType.equals(candidateReturnType);
			case ANY_ASSIGNABLE ->
					areTypesAssignableEitherDirection(referenceReturnType, candidateReturnType, inheritanceGraph);
		};
	}

	/**
	 * Compute the <a href="https://en.wikipedia.org/wiki/Dice-S%C3%B8rensen_coefficient">Dice-Sørensen coefficient</a>
	 * for two multisets of trigrams.
	 *
	 * @param left
	 * 		Reference trigrams multiset.
	 * @param right
	 * 		Candidate trigrams multiset.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double sorensenDice(@Nonnull Object2IntMap<String> left, @Nonnull Object2IntMap<String> right) {
		if (left.isEmpty() && right.isEmpty())
			return 1D;

		// Sum the counts of each trigram in both multisets to get the total size of each multiset.
		int leftTotal = left.sum();
		int rightTotal = right.sum();
		if (leftTotal + rightTotal == 0)
			return 1D;

		// Sum the minimum counts of each trigram in both multisets to get the intersection size.
		IntBox intersection = new IntBox(0);
		left.forEach((k, v) -> intersection.increment(Math.min(v, right.getOrDefault(k, 0))));

		// Compute coefficient: (2 * intersection) / (left + right)
		return (2D * intersection.get()) / (leftTotal + rightTotal);
	}

	/**
	 * @param left
	 * 		Control-flow vector of the reference method.
	 * @param right
	 * 		Control-flow vector of the candidate method.
	 *
	 * @return Normalized similarity score in {@code [0.0, 1.0]} based on the difference between the two vectors.
	 */
	private static double compareVectors(@Nonnull long[] left, @Nonnull long[] right) {
		if (left.length != right.length)
			throw new IllegalArgumentException("Control-flow vectors must be the same size");
		double total = 0.0;
		for (int i = 0; i < left.length; i++) {
			long a = left[i];
			long b = right[i];
			total += 1.0 - (Math.abs(a - b) / (double) Math.max(Math.max(a, b), 1L));
		}
		return total / left.length;
	}

	/**
	 * @param referenceParameters
	 * 		Parameter types of the reference method.
	 * @param candidateParameters
	 * 		Parameter types of the candidate method.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	static double parameterScore(@Nonnull Type[] referenceParameters, @Nonnull Type[] candidateParameters) {
		// Map parameters to tokenized descriptors.
		List<String> referenceDescriptors = descriptors(referenceParameters);
		List<String> candidateDescriptors = descriptors(candidateParameters);
		int maxCount = Math.max(Math.max(referenceDescriptors.size(), candidateDescriptors.size()), 1);

		// Score ordered parameter matches.
		int orderedMatches = 0;
		for (int i = 0; i < Math.min(referenceDescriptors.size(), candidateDescriptors.size()); i++)
			if (referenceDescriptors.get(i).equals(candidateDescriptors.get(i)))
				orderedMatches++;
		double orderedScore = referenceDescriptors.isEmpty() && candidateDescriptors.isEmpty() ? 1D : orderedMatches / (double) maxCount;

		// Score any-order parameter matches by treating the descriptors as multisets and comparing their intersection size.
		Object2IntMap<String> referenceMultiset = MethodInstructionNormalizer.multiset(referenceDescriptors);
		Object2IntMap<String> candidateMultiset = MethodInstructionNormalizer.multiset(candidateDescriptors);
		IntBox intersection = new IntBox(0);
		referenceMultiset.forEach((k, v) -> intersection.increment(Math.min(v, candidateMultiset.getOrDefault(k, 0))));
		double anyOrderScore = referenceDescriptors.isEmpty() && candidateDescriptors.isEmpty() ? 1D : intersection.get() / (double) maxCount;

		return Math.max(orderedScore, anyOrderScore);
	}

	/**
	 * @param referenceReturnType
	 * 		Return type of the reference method.
	 * @param candidateReturnType
	 * 		Return type of the candidate method.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	static double returnScore(@Nonnull Type referenceReturnType,
	                          @Nonnull Type candidateReturnType,
	                          @Nonnull InheritanceGraph inheritanceGraph) {
		if (referenceReturnType.equals(candidateReturnType))
			return 1D;
		return areTypesAssignableEitherDirection(referenceReturnType, candidateReturnType, inheritanceGraph) ?
				RETURN_ASSIGNABLE_SCORE : 0D;
	}

	/**
	 * @param left
	 * 		Some type.
	 * @param right
	 * 		Some other type.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return {@code true} if either type can be assigned to the other. {@code false} otherwise.
	 */
	static boolean areTypesAssignableEitherDirection(@Nonnull Type left,
	                                                 @Nonnull Type right,
	                                                 @Nonnull InheritanceGraph inheritanceGraph) {
		return isAssignable(left, right, inheritanceGraph) || isAssignable(right, left, inheritanceGraph);
	}

	/**
	 * @param left
	 * 		Some target type.
	 * @param right
	 * 		Some value type.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return {@code true} if the value type can be assigned to the target type, {@code false} otherwise.
	 */
	private static boolean isAssignable(@Nonnull Type left, @Nonnull Type right, @Nonnull InheritanceGraph inheritanceGraph) {
		if (left.equals(right))
			return true;
		if (Types.isPrimitive(left) || Types.isPrimitive(right))
			return false;
		if (left.getSort() == Type.VOID || right.getSort() == Type.VOID)
			return false;
		if (left.getSort() == Type.ARRAY || right.getSort() == Type.ARRAY)
			return isArrayAssignable(left, right, inheritanceGraph);
		if (left.getSort() != Type.OBJECT || right.getSort() != Type.OBJECT)
			return false;
		return inheritanceGraph.isAssignableFrom(left.getInternalName(), right.getInternalName());
	}

	/**
	 * @param left
	 * 		Some target type.
	 * @param right
	 * 		Some value type.
	 * @param inheritanceGraph
	 * 		Inheritance graph of the workspace.
	 *
	 * @return {@code true} if the value type can be assigned to the target type, {@code false} otherwise.
	 */
	private static boolean isArrayAssignable(@Nonnull Type left, @Nonnull Type right, @Nonnull InheritanceGraph inheritanceGraph) {
		if (left.equals(right))
			return true;
		if (left.getSort() == Type.OBJECT && right.getSort() == Type.ARRAY)
			return Types.isArraySuperType(left.getInternalName());
		if (left.getSort() != Type.ARRAY || right.getSort() != Type.ARRAY)
			return false;
		if (left.getDimensions() != right.getDimensions())
			return false;
		Type targetElement = left.getElementType();
		Type valueElement = right.getElementType();
		if (Types.isPrimitive(targetElement) || Types.isPrimitive(valueElement))
			return targetElement.equals(valueElement);
		return inheritanceGraph.isAssignableFrom(targetElement.getInternalName(), valueElement.getInternalName());
	}

	/**
	 * Compute the <a href="https://en.wikipedia.org/wiki/Jaccard_index">Jaccard index</a>
	 * for two sets of internal types.
	 *
	 * @param left
	 * 		Some set of internal type names.
	 * @param right
	 * 		Another set of internal type names.
	 *
	 * @return Normalized similarity score, {@code [0.0, 1.0]}.
	 */
	private static double jaccard(@Nonnull Set<String> left, @Nonnull Set<String> right) {
		if (left.isEmpty() && right.isEmpty())
			return 1D;

		Set<String> union = new HashSet<>(left);
		union.addAll(right);

		Set<String> intersection = new HashSet<>(left);
		intersection.retainAll(right);

		// Compute: intersection / union
		return union.isEmpty() ? 1D : intersection.size() / (double) union.size();
	}

	@Nonnull
	private static List<String> descriptors(@Nonnull Type[] types) {
		return Stream.of(types)
				.map(Type::getDescriptor)
				.toList();
	}
}
