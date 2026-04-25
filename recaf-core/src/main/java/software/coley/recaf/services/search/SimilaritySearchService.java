package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.search.similarity.ClassSimilarityScorer;
import software.coley.recaf.services.search.similarity.MethodFingerprint;
import software.coley.recaf.services.search.similarity.MethodFingerprinting;
import software.coley.recaf.services.search.similarity.MethodSimilarityScorer;
import software.coley.recaf.services.search.similarity.SimilarClassScoreBreakdown;
import software.coley.recaf.services.search.similarity.SimilarClassSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarClassSearchResult;
import software.coley.recaf.services.search.similarity.SimilarClassSearchScope;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchResult;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Similarity search service.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SimilaritySearchService implements Service {
	public static final String SERVICE_ID = "similar-method-search";
	private final SimilaritySearchServiceConfig config;
	private final InheritanceGraphService inheritanceGraphService;

	@Inject
	public SimilaritySearchService(@Nonnull SimilaritySearchServiceConfig config,
	                               @Nonnull InheritanceGraphService inheritanceGraphService) {
		this.config = config;
		this.inheritanceGraphService = inheritanceGraphService;
	}

	/**
	 * @param referenceMethodPath
	 * 		Path to the reference method.
	 * @param options
	 * 		Search options.
	 *
	 * @return Similar method matches.
	 */
	@Nonnull
	public List<SimilarMethodSearchResult> searchMethods(@Nonnull ClassMemberPathNode referenceMethodPath,
	                                                     @Nonnull SimilarMethodSearchOptions options) {
		return searchMethods(referenceMethodPath, options, SearchFeedback.DEFAULT);
	}

	/**
	 * @param referenceMethodPath
	 * 		Path to the reference method.
	 * @param options
	 * 		Search options.
	 * @param feedback
	 * 		Search feedback for cancellation and class filtering.
	 *
	 * @return Similar method matches.
	 */
	@Nonnull
	public List<SimilarMethodSearchResult> searchMethods(@Nonnull ClassMemberPathNode referenceMethodPath,
	                                                     @Nonnull SimilarMethodSearchOptions options,
	                                                     @Nonnull SearchFeedback feedback) {
		if (!referenceMethodPath.isMethod())
			throw new IllegalArgumentException("Path does not point to a method");
		ClassPathNode referenceClassPath = referenceMethodPath.getParent();
		if (referenceClassPath == null)
			throw new IllegalArgumentException("Reference method path is missing its declaring class");
		ClassInfo referenceClass = referenceClassPath.getValue();

		Workspace workspace = referenceMethodPath.getValueOfType(Workspace.class);
		WorkspaceResource referenceResource = referenceMethodPath.getValueOfType(WorkspaceResource.class);
		Bundle<?> referenceBundle = referenceMethodPath.getValueOfType(Bundle.class);
		MethodMember referenceMethod = (MethodMember) referenceMethodPath.getValue();
		if (workspace == null || referenceResource == null || referenceBundle == null)
			throw new IllegalArgumentException("Reference method path is missing workspace/resource/bundle context");

		InheritanceGraph inheritanceGraph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		MethodFingerprinting.Lookup referenceMethods = MethodFingerprinting.lookupFor(referenceClass);
		MethodFingerprint referenceFingerprint = referenceMethods.fingerprint(referenceMethod);
		if (referenceFingerprint == null)
			return List.of();

		List<SimilarMethodSearchResult> matches = new ArrayList<>();
		try {
			for (WorkspaceResource resource : resolveMethodCandidateResources(workspace, options)) {
				if (feedback.hasRequestedCancellation())
					break;
				searchResource(workspace, resource, feedback, (classInfo, classPath) -> {
					MethodFingerprinting.Lookup lookup = MethodFingerprinting.lookupFor(classInfo);
					for (MethodMember candidateMethod : classInfo.getMethods()) {
						if (feedback.hasRequestedCancellation())
							return false;

						ClassMemberPathNode methodPath = classPath.child(candidateMethod);
						if (methodPath.equals(referenceMethodPath))
							continue;
						if (!MethodSimilarityScorer.passesPrefilters(referenceFingerprint, candidateMethod, options, inheritanceGraph))
							continue;

						MethodFingerprint candidateFingerprint = lookup.fingerprint(candidateMethod);
						if (candidateFingerprint == null)
							continue;

						double similarity = MethodSimilarityScorer.score(referenceFingerprint, candidateFingerprint, inheritanceGraph);
						if (similarity * 100D < options.similarityThresholdPercent())
							continue;

						matches.add(new SimilarMethodSearchResult(methodPath, similarity));
					}
					return true;
				});
			}
			matches.sort(Comparator.comparingDouble(SimilarMethodSearchResult::similarity).reversed());
			return List.copyOf(matches);
		} finally {
			feedback.onCompletion();
		}
	}

	/**
	 * @param referenceClassPath
	 * 		Path to the reference class.
	 * @param options
	 * 		Search options.
	 *
	 * @return Similar class matches.
	 */
	@Nonnull
	public List<SimilarClassSearchResult> searchClasses(@Nonnull ClassPathNode referenceClassPath,
	                                                    @Nonnull SimilarClassSearchOptions options) {
		return searchClasses(referenceClassPath, options, SearchFeedback.DEFAULT);
	}

	/**
	 * @param referenceClassPath
	 * 		Path to the reference class.
	 * @param options
	 * 		Search options.
	 * @param feedback
	 * 		Search feedback for cancellation and class filtering.
	 *
	 * @return Similar class matches.
	 */
	@Nonnull
	public List<SimilarClassSearchResult> searchClasses(@Nonnull ClassPathNode referenceClassPath,
	                                                    @Nonnull SimilarClassSearchOptions options,
	                                                    @Nonnull SearchFeedback feedback) {
		Workspace workspace = referenceClassPath.getValueOfType(Workspace.class);
		WorkspaceResource referenceResource = referenceClassPath.getValueOfType(WorkspaceResource.class);
		if (workspace == null || referenceResource == null)
			throw new IllegalArgumentException("Reference class path is missing workspace/resource context");

		InheritanceGraph inheritanceGraph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		ClassSimilarityScorer.ClassComparisonModel referenceClass = ClassSimilarityScorer.buildModel(referenceClassPath.getValue());

		List<SimilarClassSearchResult> matches = new ArrayList<>();
		try {
			for (WorkspaceResource resource : resolveClassCandidateResources(workspace, referenceResource, options.searchScope())) {
				if (feedback.hasRequestedCancellation())
					break;
				searchResource(workspace, resource, feedback, (classInfo, classPath) -> {
					if (classPath.equals(referenceClassPath))
						return true;

					ClassSimilarityScorer.ClassComparisonModel candidateClass = ClassSimilarityScorer.buildModel(classInfo);
					SimilarClassScoreBreakdown breakdown = ClassSimilarityScorer.breakdown(referenceClass, candidateClass,
							options, inheritanceGraph);
					double similarity = ClassSimilarityScorer.score(breakdown, options);
					if (similarity * 100D < options.similarityThresholdPercent())
						return true;

					matches.add(new SimilarClassSearchResult(classPath, similarity, breakdown));
					return true;
				});
			}
			matches.sort(Comparator.comparingDouble(SimilarClassSearchResult::similarity).reversed());
			return List.copyOf(matches);
		} finally {
			feedback.onCompletion();
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public SimilaritySearchServiceConfig getServiceConfig() {
		return config;
	}

	private void searchResource(@Nonnull Workspace workspace,
	                            @Nonnull WorkspaceResource resource,
	                            @Nonnull SearchFeedback feedback,
	                            @Nonnull BiPredicate<ClassInfo, ClassPathNode> classAction) {
		searchBundles(workspace, resource, resource.classBundleStream().toList(), feedback, classAction);

		for (WorkspaceFileResource embeddedResource : resource.getEmbeddedResources().values()) {
			if (feedback.hasRequestedCancellation())
				return;
			searchResource(workspace, embeddedResource, feedback, classAction);
		}
	}

	private void searchBundles(@Nonnull Workspace workspace,
	                           @Nonnull WorkspaceResource resource,
	                           @Nonnull Collection<ClassBundle<? extends ClassInfo>> bundles,
	                           @Nonnull SearchFeedback feedback,
	                           @Nonnull BiPredicate<ClassInfo, ClassPathNode> classAction) {
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		for (ClassBundle<? extends ClassInfo> bundle : bundles) {
			BundlePathNode bundlePath = resourcePath.child(bundle);
			for (ClassInfo classInfo : bundle) {
				if (feedback.hasRequestedCancellation())
					return;
				if (!feedback.doVisitClass(classInfo))
					continue;

				ClassPathNode classPath = bundlePath.child(classInfo);
				if (!classAction.test(classInfo, classPath))
					return;
			}
		}
	}

	@Nonnull
	private static List<WorkspaceResource> resolveMethodCandidateResources(@Nonnull Workspace workspace,
	                                                                       @Nonnull SimilarMethodSearchOptions options) {
		return options.primaryResourceOnly() ? List.of(workspace.getPrimaryResource()) : workspace.getAllResources(false);
	}

	@Nonnull
	private static List<WorkspaceResource> resolveClassCandidateResources(@Nonnull Workspace workspace,
	                                                                      @Nonnull WorkspaceResource referenceResource,
	                                                                      @Nonnull SimilarClassSearchScope scope) {
		LinkedHashSet<WorkspaceResource> resources = new LinkedHashSet<>();
		switch (scope.mode()) {
			case SELF_RESOURCE -> resources.add(referenceResource);
			case ALL_NON_INTERNAL -> workspace.getAllResources(false).stream()
					.filter(resource -> !resource.isInternal())
					.forEach(resources::add);
			case TARGET_RESOURCES -> scope.targetResources().stream()
					.filter(resource -> !resource.isInternal())
					.forEach(resources::add);
		}
		return List.copyOf(resources);
	}
}
