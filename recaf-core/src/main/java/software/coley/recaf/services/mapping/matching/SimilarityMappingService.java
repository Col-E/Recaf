package software.coley.recaf.services.mapping.matching;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.search.SearchFeedback;
import software.coley.recaf.services.search.similarity.MemberOrderMode;
import software.coley.recaf.services.search.similarity.MethodSimilarityScorer;
import software.coley.recaf.services.search.similarity.ParameterMatchMode;
import software.coley.recaf.services.search.similarity.ReturnMatchMode;
import software.coley.recaf.services.search.similarity.SimilarClassScoreBreakdown;
import software.coley.recaf.services.search.similarity.SimilarClassSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarClassSearchScope;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import static software.coley.recaf.services.search.similarity.ClassSimilarityScorer.*;

/**
 * Service for generating mappings from class similarity between two workspace resources.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SimilarityMappingService implements Service {
	public static final String SERVICE_ID = "similarity-mapping";

	private final SimilarityMappingServiceConfig config;
	private final InheritanceGraphService inheritanceGraphService;

	@Inject
	public SimilarityMappingService(@Nonnull SimilarityMappingServiceConfig config,
	                                @Nonnull InheritanceGraphService inheritanceGraphService) {

		this.config = config;
		this.inheritanceGraphService = inheritanceGraphService;
	}

	/**
	 * Generate mappings between source and target resources.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param sourceResource
	 * 		Resource to map from.
	 * @param targetResource
	 * 		Resource to map against.
	 * @param options
	 * 		Similarity thresholds.
	 *
	 * @return Generated mappings.
	 */
	@Nonnull
	public Mappings generate(@Nonnull Workspace workspace,
	                         @Nonnull WorkspaceResource sourceResource,
	                         @Nonnull WorkspaceResource targetResource,
	                         @Nonnull SimilarityMappingOptions options) {
		return analyze(workspace, sourceResource, targetResource, options, SearchFeedback.DEFAULT).getMappings();
	}

	/**
	 * Analyze source and target resources to produce similarity-based mappings and preview metadata.
	 *
	 * @param workspace
	 * 		Containing workspace.
	 * @param sourceResource
	 * 		Resource to map from.
	 * @param targetResource
	 * 		Resource to map against.
	 * @param options
	 * 		Similarity thresholds.
	 * @param feedback
	 * 		Cancellation feedback.
	 *
	 * @return Analysis report.
	 */
	@Nonnull
	public SimilarityMappingsReport analyze(@Nonnull Workspace workspace,
	                                        @Nonnull WorkspaceResource sourceResource,
	                                        @Nonnull WorkspaceResource targetResource,
	                                        @Nonnull SimilarityMappingOptions options,
	                                        @Nonnull SearchFeedback feedback) {
		// Collect classes in source/target resources.
		List<ClassPathNode> sourceClasses = collectClasses(workspace, sourceResource, feedback);
		List<ClassPathNode> targetClasses = collectClasses(workspace, targetResource, feedback);

		// Create comparison models for classes in both resources.
		Map<ClassPathNode, ClassComparisonModel> classModels = new HashMap<>(sourceClasses.size() + targetClasses.size());
		List<PreparedClass> preparedTargets = prepareClasses(targetClasses, classModels);

		// Bucket target resource classes by structural properties.
		// We do this to speed up candidate selection. For instance, no need to compare enum classes to non-enum classes.
		// Same idea for classes with very different field/method counts.
		Map<ClassKind, List<PreparedClass>> typeBuckets = bucketByType(preparedTargets);
		Map<StructuralBucket, List<PreparedClass>> structuralBuckets = bucketByStructure(preparedTargets);
		Map<StructuralBucket, List<PreparedClass>> nearbyBuckets = createNearbyBucketPools(structuralBuckets);

		// For each source class, select a pool of candidates to compare against and generate a match proposal.
		List<MatchProposal> proposals = new ArrayList<>(sourceClasses.size());
		List<ClassPathNode> unresolved = new ArrayList<>();
		SimilarClassSearchOptions searchOptions = createSearchOptions(options, targetResource);
		MethodSimilarityScorer methodSimilarityScorer = new MethodSimilarityScorer();
		InheritanceGraph inheritanceGraph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		for (ClassPathNode sourcePath : sourceClasses) {
			if (feedback.hasRequestedCancellation())
				break;

			// Wrap structural properties and comparison model.
			PreparedClass preparedSource = prepareClass(sourcePath, classModels);

			// Select candidate pool based on structural similarity buckets.
			List<PreparedClass> candidatePool = selectCandidatePool(preparedSource, preparedTargets, nearbyBuckets, typeBuckets, options);
			MatchProposal proposal = createProposal(preparedSource, candidatePool, searchOptions, options, methodSimilarityScorer, inheritanceGraph, feedback);
			if (proposal == null)
				unresolved.add(sourcePath);
			else
				proposals.add(proposal);
		}

		// Collect the highest matching proposal for each source class, and mark the rest as unresolved.
		Map<ClassPathNode, MatchProposal> winningProposals = new HashMap<>();
		proposals.stream()
				.collect(LinkedHashMap<ClassPathNode, List<MatchProposal>>::new,
						(map, proposal) -> map.computeIfAbsent(proposal.targetPath(), _ -> new ArrayList<>()).add(proposal),
						Map::putAll)
				.forEach((_, candidates) -> {
					candidates.sort(MatchProposal.ORDERING);
					MatchProposal winner = candidates.getFirst();
					winningProposals.put(winner.sourcePath(), winner);
					for (int i = 1; i < candidates.size(); i++)
						unresolved.add(candidates.get(i).sourcePath());
				});

		// For each accepted class match, generate member mappings for fields and methods that meet the similarity threshold.
		IntermediateMappings mappings = new IntermediateMappings();
		List<SimilarityClassMatch> acceptedMatches = new ArrayList<>(winningProposals.size());
		for (ClassPathNode sourcePath : sourceClasses) {
			MatchProposal proposal = winningProposals.get(sourcePath);
			if (proposal == null)
				continue;

			// Create similarity models for source and target classes.
			ClassComparisonModel sourceModel = classModels.get(sourcePath);
			ClassComparisonModel targetModel = classModels.get(proposal.targetPath());
			DetailedClassSimilarity detailed = detailedBreakdown(sourceModel, targetModel, searchOptions, methodSimilarityScorer, inheritanceGraph);

			// Map class.
			String sourceClassName = sourcePath.getValue().getName();
			String targetClassName = proposal.targetPath().getValue().getName();
			InheritanceVertex sourceVertex = inheritanceGraph.getVertex(sourceClassName);
			mappings.addClass(sourceClassName, targetClassName);

			// Map matching fields.
			List<SimilarityMemberMatch> fieldMatches = new ArrayList<>();
			for (FieldSimilarityMatch fieldMatch : detailed.fieldMatches()) {
				double similarityPercent = fieldMatch.similarity() * 100;
				FieldMember sourceField = fieldMatch.reference();
				FieldMember targetField = fieldMatch.candidate();
				fieldMatches.add(new SimilarityMemberMatch(
						sourceField.getName(),
						sourceField.getDescriptor(),
						targetField.getName(),
						targetField.getDescriptor(),
						fieldMatch.similarity()
				));
				if (similarityPercent >= options.memberSimilarityThresholdPercent())
					mappings.addField(sourceClassName, sourceField.getDescriptor(), sourceField.getName(), targetField.getName());
			}

			// Map matching methods.
			List<SimilarityMemberMatch> methodMatches = new ArrayList<>();
			for (MethodSimilarityMatch methodMatch : detailed.methodMatches()) {
				MethodMember sourceMethod = methodMatch.reference();
				MethodMember targetMethod = methodMatch.candidate();

				// Skip <init>/<clinit>
				if (isReservedMethod(sourceMethod) || isReservedMethod(targetMethod))
					continue;

				// Add mapping if the similarity is high enough, and it's not part of a library method family (like toString).
				double similarityPercent = methodMatch.similarity() * 100;
				methodMatches.add(new SimilarityMemberMatch(
						sourceMethod.getName(),
						sourceMethod.getDescriptor(),
						targetMethod.getName(),
						targetMethod.getDescriptor(),
						methodMatch.similarity()
				));
				if (similarityPercent >= options.memberSimilarityThresholdPercent()
						&& !isLibraryMethodFamily(sourceVertex, sourceMethod.getName(), sourceMethod.getDescriptor())
						&& !isLibraryMethodFamily(sourceVertex, targetMethod.getName(), sourceMethod.getDescriptor())) {
					mappings.addMethod(sourceClassName, sourceMethod.getDescriptor(), sourceMethod.getName(), targetMethod.getName());
				}
			}

			acceptedMatches.add(new SimilarityClassMatch(
					sourcePath,
					proposal.targetPath(),
					proposal.similarity(),
					proposal.certaintyGap(),
					fieldMatches,
					methodMatches
			));
		}

		List<ClassPathNode> unresolvedSorted = unresolved.stream()
				.distinct()
				.sorted(Comparator.comparing(path -> path.getValue().getName()))
				.toList();
		return new SimilarityMappingsReport(mappings, acceptedMatches, unresolvedSorted);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public SimilarityMappingServiceConfig getServiceConfig() {
		return config;
	}

	@Nonnull
	private static SimilarClassSearchOptions createSearchOptions(@Nonnull SimilarityMappingOptions options,
	                                                             @Nonnull WorkspaceResource targetResource) {
		// For similarity mapping we only allow a few parameters to be configured with options.
		// Some values are going to be strict (like return type / parameter matching) since it wouldn't make sense
		// to draw parallels between things that don't have matching signatures.
		return new SimilarClassSearchOptions(
				options.classSimilarityThresholdPercent(),
				ParameterMatchMode.EXACT_COUNT_AND_ORDER,
				ReturnMatchMode.EXACT_TYPE,
				MemberOrderMode.PERMUTATION,
				MemberOrderMode.PERMUTATION,
				SimilarClassSearchOptions.DEFAULT_METHOD_WEIGHT,
				SimilarClassSearchOptions.DEFAULT_FIELD_WEIGHT,
				SimilarClassSearchScope.targetResources(List.of(targetResource))
		);
	}

	/**
	 * @param workspace
	 * 		Workspace containing the given resource.
	 * @param resource
	 * 		Resource to collect classes from, including any embedded resources.
	 * @param feedback
	 * 		Cancellation feedback to check during traversal.
	 *
	 * @return Accumulated classes from the given resource.
	 */
	@Nonnull
	private static List<ClassPathNode> collectClasses(@Nonnull Workspace workspace,
	                                                  @Nonnull WorkspaceResource resource,
	                                                  @Nonnull SearchFeedback feedback) {
		List<ClassPathNode> classes = new ArrayList<>();
		collectClasses(workspace, resource, feedback, classes);
		classes.sort(Comparator.comparing(path -> path.getValue().getName()));
		return List.copyOf(classes);
	}

	/**
	 * @param workspace
	 * 		Workspace containing the given resource.
	 * @param resource
	 * 		Resource to collect classes from, including any embedded resources.
	 * @param feedback
	 * 		Cancellation feedback to check during traversal.
	 * @param sink
	 * 		Sink to accumulate collected class paths into.
	 */
	private static void collectClasses(@Nonnull Workspace workspace,
	                                   @Nonnull WorkspaceResource resource,
	                                   @Nonnull SearchFeedback feedback,
	                                   @Nonnull List<ClassPathNode> sink) {
		// TODO: This manual traversal is not great, but the workspace 'find' operations are not
		//  scoped down to resources. Perhaps we can refactor it later to facilitate that and
		//  move some traversal handling there.
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		for (ClassBundle<? extends ClassInfo> bundle : resource.classBundleStream().toList()) {
			if (feedback.hasRequestedCancellation())
				return;
			BundlePathNode bundlePath = resourcePath.child(bundle);
			for (ClassInfo classInfo : bundle) {
				if (feedback.hasRequestedCancellation())
					return;
				if (!feedback.doVisitClass(classInfo))
					continue;
				sink.add(bundlePath.child(classInfo));
			}
		}
		for (WorkspaceFileResource embeddedResource : resource.getEmbeddedResources().values()) {
			if (feedback.hasRequestedCancellation())
				return;
			collectClasses(workspace, embeddedResource, feedback, sink);
		}
	}

	/**
	 * @param method
	 * 		Method to check.
	 *
	 * @return {@code true} for constructors and static initializers, {@code false} otherwise.
	 */
	private static boolean isReservedMethod(@Nonnull MethodMember method) {
		String name = method.getName();
		return "<init>".equals(name) || "<clinit>".equals(name);
	}

	/**
	 * @param vertex
	 * 		Parent class vertex to check against.
	 * @param methodName
	 * 		Method name to check.
	 * @param methodDesc
	 * 		Method descriptor to check.
	 *
	 * @return {@code true} if the method is part of a library method family, {@code false} otherwise.
	 */
	private static boolean isLibraryMethodFamily(@Nullable InheritanceVertex vertex,
	                                             @Nonnull String methodName,
	                                             @Nonnull String methodDesc) {
		return vertex != null && vertex.isLibraryMethod(methodName, methodDesc);
	}

	/**
	 * @param classes
	 * 		Input classes to prepare.
	 * @param classModels
	 * 		Cache of class comparison models to populate and reuse.
	 *
	 * @return Prepared models with structural buckets and profiles for candidate selection and scoring.
	 */
	@Nonnull
	private static List<PreparedClass> prepareClasses(@Nonnull List<ClassPathNode> classes,
	                                                  @Nonnull Map<ClassPathNode, ClassComparisonModel> classModels) {
		List<PreparedClass> prepared = new ArrayList<>(classes.size());
		for (ClassPathNode classPath : classes)
			prepared.add(prepareClass(classPath, classModels));
		return List.copyOf(prepared);
	}

	/**
	 * @param classPath
	 * 		Class to prepare.
	 * @param classModels
	 * 		Cache of class comparison models to populate and reuse.
	 *
	 * @return Prepared model with structural bucket and profile for candidate selection and scoring.
	 */
	@Nonnull
	private static PreparedClass prepareClass(@Nonnull ClassPathNode classPath,
	                                          @Nonnull Map<ClassPathNode, ClassComparisonModel> classModels) {
		ClassComparisonModel model = classModels.computeIfAbsent(
				classPath,
				_ -> buildModel(classPath.getValue()));
		ClassInfo classInfo = classPath.getValue();
		return new PreparedClass(classPath, model, StructuralBucket.from(classInfo), StructuralProfile.from(classInfo));
	}

	/**
	 * @param classes
	 * 		Input classes to bucket.
	 *
	 * @return Map of structural buckets to classes.
	 */
	@Nonnull
	private static Map<StructuralBucket, List<PreparedClass>> bucketByStructure(@Nonnull List<PreparedClass> classes) {
		Map<StructuralBucket, List<PreparedClass>> buckets = new HashMap<>();
		for (PreparedClass preparedClass : classes) {
			buckets.computeIfAbsent(preparedClass.bucket(), _ -> new ArrayList<>()).add(preparedClass);
		}
		return buckets;
	}

	/**
	 * @param structuralBuckets
	 * 		Structural buckets to create nearby pools from.
	 *
	 * @return Map of structural buckets to nearby candidate pools.
	 *
	 * @see StructuralBucket#isNear(StructuralBucket)
	 */
	@Nonnull
	private static Map<StructuralBucket, List<PreparedClass>> createNearbyBucketPools(@Nonnull Map<StructuralBucket, List<PreparedClass>> structuralBuckets) {
		Map<StructuralBucket, List<PreparedClass>> pools = new HashMap<>();
		for (StructuralBucket sourceBucket : structuralBuckets.keySet()) {
			// Find all buckets that are "near" the source bucket and pool their classes together for candidate selection.
			List<PreparedClass> nearby = new ArrayList<>();
			for (Map.Entry<StructuralBucket, List<PreparedClass>> entry : structuralBuckets.entrySet())
				if (sourceBucket.isNear(entry.getKey()))
					nearby.addAll(entry.getValue());
			pools.put(sourceBucket, List.copyOf(nearby));
		}
		return pools;
	}

	/**
	 * @param classes
	 * 		Input classes to bucket.
	 *
	 * @return Map of class kinds to classes.
	 */
	@Nonnull
	private static Map<ClassKind, List<PreparedClass>> bucketByType(@Nonnull List<PreparedClass> classes) {
		Map<ClassKind, List<PreparedClass>> buckets = new HashMap<>();
		for (PreparedClass preparedClass : classes)
			buckets.computeIfAbsent(preparedClass.bucket().kind(), _ -> new ArrayList<>()).add(preparedClass);
		return buckets;
	}

	/**
	 * @param source
	 * 		Source class to select candidates for.
	 * @param allTargets
	 * 		All target classes to select from if no better buckets are found.
	 * @param nearbyBuckets
	 * 		Map of structural buckets to nearby candidate pools.
	 * @param typeBuckets
	 * 		Map of class kinds to classes.
	 * @param options
	 * 		Similarity mapping options for shortlist tuning.
	 *
	 * @return Candidate pool for the source class.
	 */
	@Nonnull
	private static List<PreparedClass> selectCandidatePool(@Nonnull PreparedClass source,
	                                                       @Nonnull List<PreparedClass> allTargets,
	                                                       @Nonnull Map<StructuralBucket, List<PreparedClass>> nearbyBuckets,
	                                                       @Nonnull Map<ClassKind, List<PreparedClass>> typeBuckets,
	                                                       @Nonnull SimilarityMappingOptions options) {
		// Check for nearby structural buckets first since that is more likely to yield higher similarity matches.
		List<PreparedClass> nearbyMatches = nearbyBuckets.get(source.bucket());
		if (nearbyMatches != null && !nearbyMatches.isEmpty())
			return shortlistCandidates(source, nearbyMatches, options);

		// None? Let's try same-type buckets.
		List<PreparedClass> sameTypeMatches = typeBuckets.get(source.bucket().kind());
		if (sameTypeMatches != null && !sameTypeMatches.isEmpty())
			return shortlistCandidates(source, sameTypeMatches, options);

		// Still nothing? Check everything then.
		return shortlistCandidates(source, allTargets, options);
	}

	/**
	 * @param source
	 * 		Source class to score candidates against.
	 * @param candidates
	 * 		Candidate classes to score and shortlist.
	 * @param options
	 * 		Similarity mapping options for shortlist tuning.
	 *
	 * @return Top candidates based on structural profile similarity, capped at {@link SimilarityMappingOptions#maxFullScoreCandidates()}.
	 */
	@Nonnull
	private static List<PreparedClass> shortlistCandidates(@Nonnull PreparedClass source,
	                                                       @Nonnull List<PreparedClass> candidates,
	                                                       @Nonnull SimilarityMappingOptions options) {
		int maxFullScoreCandidates = options.maxFullScoreCandidates();
		if (candidates.size() <= maxFullScoreCandidates)
			return candidates;

		// We want to cut down the number of candidates, so we'll do a quick pre-screening
		// based on structural profile similarity and keep the top N candidates.
		double threshold = options.shortlistGapThresholdPercent() / 100.0;
		double maxScore = Double.NEGATIVE_INFINITY;
		Comparator<ScoredPreparedClass> ordering = Comparator
				.comparingDouble(ScoredPreparedClass::score)
				.thenComparing(ScoredPreparedClass::name, Comparator.reverseOrder());
		Queue<ScoredPreparedClass> shortlist = new PriorityQueue<>(maxFullScoreCandidates + 1, ordering);
		for (PreparedClass candidate : candidates) {
			// Compare structure profiles and add to shortlist if it's one of the top candidates we've seen so far.
			// If it is a significant increase over the current max score drop everything we've seen so far.
			double score = source.profile().similarity(candidate.profile());
			if (score > maxScore) {
				if (score > maxScore + threshold)
					shortlist.clear();
				maxScore = score;
			} else if (score < maxScore - threshold) {
				continue;
			}
			ScoredPreparedClass scored = new ScoredPreparedClass(candidate, score);
			if (shortlist.size() < maxFullScoreCandidates) {
				shortlist.offer(scored);
				continue;
			}

			// If the shortlist is full, only add the candidate if it has a higher score
			// than the worst candidate currently in the shortlist.
			ScoredPreparedClass worst = shortlist.peek();
			if (ordering.compare(scored, worst) > 0) {
				shortlist.poll();
				shortlist.offer(scored);
			}
		}

		// Now we sort our shortlist and we're done.
		List<ScoredPreparedClass> sorted = new ArrayList<>(shortlist);
		sorted.sort(Comparator
				.comparingDouble(ScoredPreparedClass::score).reversed()
				.thenComparing(scored -> scored.preparedClass().path().getValue().getName()));
		return sorted.stream()
				.map(ScoredPreparedClass::preparedClass)
				.toList();
	}

	/**
	 * @param source
	 * 		Source class to score candidates against.
	 * @param targets
	 * 		Candidate classes to score and select the best match from.
	 * @param searchOptions
	 * 		Options to use for similarity scoring.
	 * @param options
	 * 		Similarity mapping options for threshold values.
	 * @param methodSimilarityScorer
	 * 		Method similarity scorer to use for fingerprint comparisons.
	 * @param inheritanceGraph
	 * 		Inheritance graph to use for scoring.
	 * @param feedback
	 * 		Cancellation feedback.
	 *
	 * @return Match proposal for the source class, or {@code null} if no match meets the acceptance criteria.
	 */
	@Nullable
	private static MatchProposal createProposal(@Nonnull PreparedClass source,
	                                            @Nonnull List<PreparedClass> targets,
	                                            @Nonnull SimilarClassSearchOptions searchOptions,
	                                            @Nonnull SimilarityMappingOptions options,
	                                            @Nonnull MethodSimilarityScorer methodSimilarityScorer,
	                                            @Nonnull InheritanceGraph inheritanceGraph,
	                                            @Nonnull SearchFeedback feedback) {
		// Track the top two matches for calculating the certainty gap.
		// We want to ensure the best match is sufficiently better than the runner-up to be accepted.
		// Callers can disable this by setting the certainty gap to 0, but generally that's not recommended.
		PreparedClass topTarget = null;
		double topSimilarity = Double.NEGATIVE_INFINITY;
		double secondSimilarity = Double.NEGATIVE_INFINITY;
		for (PreparedClass target : targets) {
			if (feedback.hasRequestedCancellation())
				return null;

			// Compute similarity score for the source-target pair and update the top matches if necessary.
			SimilarClassScoreBreakdown breakdown = breakdown(source.model(), target.model(), searchOptions, methodSimilarityScorer, inheritanceGraph);
			double similarity = score(breakdown, searchOptions);
			if (similarity > topSimilarity) {
				secondSimilarity = topSimilarity;
				topSimilarity = similarity;
				topTarget = target;
			} else if (similarity > secondSimilarity) {
				secondSimilarity = similarity;
			}
		}

		// No top match? No proposal.
		if (topTarget == null)
			return null;

		// Top match is not similar enough? No proposal.
		double similarityPercent = topSimilarity * 100;
		if (similarityPercent < options.classSimilarityThresholdPercent())
			return null;

		// Top match is not sufficiently better than the runner-up? No proposal.
		double certaintyGap = secondSimilarity > Double.NEGATIVE_INFINITY
				? (topSimilarity - secondSimilarity)
				: 1.0;
		if (certaintyGap * 100 < options.classCertaintyGapPercent())
			return null;

		return new MatchProposal(source.path(), topTarget.path(), topSimilarity, certaintyGap);
	}

	/**
	 * Wrapper for {@link #shortlistCandidates(PreparedClass, List, SimilarityMappingOptions)}
	 *
	 * @param preparedClass
	 * 		Wrapped class.
	 * @param score
	 * 		Similarity score for the class with the source class.
	 */
	private record ScoredPreparedClass(@Nonnull PreparedClass preparedClass, double score) {
		@Nonnull
		private String name() {
			return preparedClass.name();
		}

		@Nonnull
		@Override
		public String toString() {
			return name() + " : " + score;
		}
	}

	/**
	 * Wrapper for prepared class data used during candidate selection and scoring.
	 *
	 * @param path
	 * 		Class path.
	 * @param model
	 * 		Class comparison model.
	 * @param bucket
	 * 		Structural bucket for candidate selection.
	 * @param profile
	 * 		Structural profile for candidate pre-screening and scoring.
	 */
	private record PreparedClass(@Nonnull ClassPathNode path,
	                             @Nonnull ClassComparisonModel model,
	                             @Nonnull StructuralBucket bucket,
	                             @Nonnull StructuralProfile profile) {
		@Nonnull
		private String name() {
			return path.getValue().getName();
		}
	}

	/**
	 * Structural properties of a class used for candidate selection and pre-screening.
	 *
	 * @param kind
	 * 		Class kind <i>(enum, interface, etc.)</i> for coarse-grained bucketing.
	 * @param fieldBucket
	 * 		Bucketed field count for mid-grained bucketing.
	 * @param methodBucket
	 * 		Bucketed method count for mid-grained bucketing.
	 */
	private record StructuralBucket(@Nonnull ClassKind kind,
	                                @Nonnull CountBucket fieldBucket,
	                                @Nonnull CountBucket methodBucket) {
		@Nonnull
		private static StructuralBucket from(@Nonnull ClassInfo classInfo) {
			return new StructuralBucket(
					ClassKind.from(classInfo),
					CountBucket.from(classInfo.getFields().size()),
					CountBucket.from(classInfo.getMethods().size())
			);
		}

		/**
		 * @param other
		 * 		Some other bucket to compare against.
		 *
		 * @return {@code true} when the kinds match, and the count buckets are within 1 distance.
		 */
		private boolean isNear(@Nonnull StructuralBucket other) {
			return kind == other.kind
					&& fieldBucket.distance(other.fieldBucket) <= 1
					&& methodBucket.distance(other.methodBucket) <= 1;
		}
	}

	/**
	 * Structural properties of a class used for candidate pre-screening and scoring.
	 */
	private record StructuralProfile(int fieldCount,
	                                 int methodCount,
	                                 int constructorCount,
	                                 int staticFieldCount,
	                                 int staticMethodCount,
	                                 int abstractMethodCount,
	                                 int primitiveFieldCount,
	                                 int objectFieldCount,
	                                 int arrayFieldCount,
	                                 int voidReturnCount,
	                                 int primitiveReturnCount,
	                                 int objectReturnCount,
	                                 int arrayReturnCount,
	                                 int zeroArgMethodCount,
	                                 int oneArgMethodCount,
	                                 int twoArgMethodCount,
	                                 int threePlusArgMethodCount) {
		/** Computed number of components for the profile. */
		static final int components = StructuralProfile.class.getRecordComponents().length;

		@Nonnull
		private static StructuralProfile from(@Nonnull ClassInfo classInfo) {
			int fieldCount = classInfo.getFields().size();
			int methodCount = classInfo.getMethods().size();
			int constructorCount = 0;
			int staticFieldCount = 0;
			int staticMethodCount = 0;
			int abstractMethodCount = 0;
			int primitiveFieldCount = 0;
			int objectFieldCount = 0;
			int arrayFieldCount = 0;
			int voidReturnCount = 0;
			int primitiveReturnCount = 0;
			int objectReturnCount = 0;
			int arrayReturnCount = 0;
			int zeroArgMethodCount = 0;
			int oneArgMethodCount = 0;
			int twoArgMethodCount = 0;
			int threePlusArgMethodCount = 0;

			for (FieldMember field : classInfo.getFields()) {
				if (field.hasStaticModifier())
					staticFieldCount++;
				switch (classify(Type.getType(field.getDescriptor()))) {
					case PRIMITIVE -> primitiveFieldCount++;
					case OBJECT -> objectFieldCount++;
					case ARRAY -> arrayFieldCount++;
					case VOID -> {}
				}
			}

			for (MethodMember method : classInfo.getMethods()) {
				if (method.hasStaticModifier())
					staticMethodCount++;
				if (method.hasAbstractModifier())
					abstractMethodCount++;
				if (isReservedMethod(method))
					constructorCount++;

				Type methodType = Type.getMethodType(method.getDescriptor());
				switch (classify(methodType.getReturnType())) {
					case VOID -> voidReturnCount++;
					case PRIMITIVE -> primitiveReturnCount++;
					case OBJECT -> objectReturnCount++;
					case ARRAY -> arrayReturnCount++;
				}

				int parameterCount = methodType.getArgumentTypes().length;
				if (parameterCount == 0)
					zeroArgMethodCount++;
				else if (parameterCount == 1)
					oneArgMethodCount++;
				else if (parameterCount == 2)
					twoArgMethodCount++;
				else
					threePlusArgMethodCount++;
			}

			return new StructuralProfile(
					fieldCount,
					methodCount,
					constructorCount,
					staticFieldCount,
					staticMethodCount,
					abstractMethodCount,
					primitiveFieldCount,
					objectFieldCount,
					arrayFieldCount,
					voidReturnCount,
					primitiveReturnCount,
					objectReturnCount,
					arrayReturnCount,
					zeroArgMethodCount,
					oneArgMethodCount,
					twoArgMethodCount,
					threePlusArgMethodCount
			);
		}

		private double similarity(@Nonnull StructuralProfile other) {
			double total = 0;
			total += closeness(fieldCount, other.fieldCount);
			total += closeness(methodCount, other.methodCount);
			total += closeness(constructorCount, other.constructorCount);
			total += closeness(staticFieldCount, other.staticFieldCount);
			total += closeness(staticMethodCount, other.staticMethodCount);
			total += closeness(abstractMethodCount, other.abstractMethodCount);
			total += closeness(primitiveFieldCount, other.primitiveFieldCount);
			total += closeness(objectFieldCount, other.objectFieldCount);
			total += closeness(arrayFieldCount, other.arrayFieldCount);
			total += closeness(voidReturnCount, other.voidReturnCount);
			total += closeness(primitiveReturnCount, other.primitiveReturnCount);
			total += closeness(objectReturnCount, other.objectReturnCount);
			total += closeness(arrayReturnCount, other.arrayReturnCount);
			total += closeness(zeroArgMethodCount, other.zeroArgMethodCount);
			total += closeness(oneArgMethodCount, other.oneArgMethodCount);
			total += closeness(twoArgMethodCount, other.twoArgMethodCount);
			total += closeness(threePlusArgMethodCount, other.threePlusArgMethodCount);
			return total / components;
		}

		@Nonnull
		private static ValueKind classify(@Nonnull Type type) {
			return switch (type.getSort()) {
				case Type.VOID -> ValueKind.VOID;
				case Type.ARRAY -> ValueKind.ARRAY;
				case Type.OBJECT -> ValueKind.OBJECT;
				default -> ValueKind.PRIMITIVE;
			};
		}

		private static double closeness(int left, int right) {
			if (left == right)
				return 1;

			// Normalize distance to percent similarity.
			int max = Math.max(Math.max(left, right), 1);
			return Math.max(0, 1 - (Math.abs(left - right) / (double) max));
		}
	}

	/**
	 * Coarse-grained value kind for bucketing.
	 */
	private enum ValueKind {
		VOID,
		PRIMITIVE,
		OBJECT,
		ARRAY
	}

	/**
	 * Coarse-grained class kind for bucketing.
	 */
	private enum ClassKind {
		CLASS,
		INTERFACE,
		ENUM,
		RECORD;

		@Nonnull
		private static ClassKind from(@Nonnull ClassInfo classInfo) {
			if (classInfo.hasEnumModifier())
				return ENUM;
			if (classInfo.hasRecordModifier())
				return RECORD;
			if (classInfo.hasInterfaceModifier())
				return INTERFACE;
			return CLASS;
		}
	}

	/**
	 * Bucketed counts to facilitate structural bucketing without too much granularity.
	 */
	private enum CountBucket {
		ZERO,
		ONE_TO_TWO,
		THREE_TO_FIVE,
		SIX_TO_TEN,
		ELEVEN_PLUS;

		@Nonnull
		private static CountBucket from(int count) {
			if (count == 0)
				return ZERO;
			if (count <= 2)
				return ONE_TO_TWO;
			if (count <= 5)
				return THREE_TO_FIVE;
			if (count <= 10)
				return SIX_TO_TEN;
			return ELEVEN_PLUS;
		}

		private int distance(@Nonnull CountBucket other) {
			return Math.abs(ordinal() - other.ordinal());
		}
	}

	/**
	 * Proposed match between a source class and a target class.
	 *
	 * @param sourcePath
	 * 		Path to the source class.
	 * @param targetPath
	 * 		Path to the target class.
	 * @param similarity
	 * 		Similarity score for the proposed match in range {@code [0.0, 1.0]}.
	 * @param certaintyGap
	 * 		Similarity gap between the top match and the runner-up in range {@code [0.0, 1.0]}.
	 */
	private record MatchProposal(@Nonnull ClassPathNode sourcePath,
	                             @Nonnull ClassPathNode targetPath,
	                             double similarity,
	                             double certaintyGap) {
		/** Descending ordering by similarity, then certainty gap, then name for tie-breaking. */
		private static final Comparator<MatchProposal> ORDERING = Comparator
				.comparingDouble(MatchProposal::similarity).reversed()
				.thenComparingDouble(MatchProposal::certaintyGap).reversed()
				.thenComparing(MatchProposal::name);

		@Nonnull
		private String name() {
			return sourcePath.getValue().getName();
		}
	}
}
