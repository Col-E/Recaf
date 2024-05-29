package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.collections.tuple.Pair;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Result wrapper for {@link MappingApplier} operations.
 * Can serve as a preview for mapping operations before updating the affected {@link Workspace}.
 * <br>
 * Use {@link #apply()} to apply the mappings to the {@link WorkspaceResource} targeted in the mapping operation.
 *
 * @author Matt Coley
 */
public class MappingResults {
	private static final Logger logger = Logging.get(MappingResults.class);
	private final Map<String, String> mappedClasses = new HashMap<>();
	private final Map<String, String> mappedClassesReverse = new HashMap<>();
	private final Map<String, ClassPathNode> preMappingPaths = new HashMap<>();
	private final Map<String, ClassPathNode> postMappingPaths = new HashMap<>();
	private final MappingApplicationListener applicationHandler;
	private final Mappings mappings;
	private AggregateMappingManager aggregateMappingManager;

	/**
	 * @param mappings
	 * 		The mappings implementation used in the operation.
	 * @param applicationHandler
	 * 		Optional handler for intercepting post/pre mapping states.
	 */
	public MappingResults(@Nonnull Mappings mappings, @Nullable MappingApplicationListener applicationHandler) {
		this.mappings = mappings;
		this.applicationHandler = applicationHandler;
	}

	/**
	 * @param aggregateMappingManager
	 * 		Aggregate mapping manager to track mapping applications in.
	 *
	 * @return Self.
	 */
	@Nonnull
	public MappingResults withAggregateManager(@Nonnull AggregateMappingManager aggregateMappingManager) {
		this.aggregateMappingManager = aggregateMappingManager;
		return this;
	}

	/**
	 * @param workspace
	 * 		Workspace containing the class.
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param preMapping
	 * 		The pre-mapped class.
	 * @param postMapping
	 * 		The post-mapped class.
	 */
	public void add(@Nonnull Workspace workspace,
					@Nonnull WorkspaceResource resource,
					@Nonnull ClassBundle<?> bundle,
					@Nonnull ClassInfo preMapping,
					@Nonnull ClassInfo postMapping) {
		String preMappingName = preMapping.getName();
		String postMappingName = postMapping.getName();
		BundlePathNode bundlePath = PathNodes.bundlePath(workspace, resource, bundle);
		ClassPathNode preMappingPath = bundlePath.child(preMapping.getPackageName()).child(preMapping);
		ClassPathNode postMappingPath = bundlePath.child(postMapping.getPackageName()).child(postMapping);
		synchronized (mappedClasses) {
			mappedClasses.put(preMappingName, postMappingName);
		}
		synchronized (mappedClassesReverse) {
			mappedClassesReverse.put(postMappingName, preMappingName);
		}
		synchronized (preMappingPaths) {
			preMappingPaths.put(preMappingName, preMappingPath);
		}
		synchronized (postMappingPaths) {
			postMappingPaths.put(postMappingName, postMappingPath);
		}
	}

	/**
	 * Applies the mappings to the {@link Workspace} / {@link WorkspaceResource} from {@link MappingApplier}.
	 */
	@SuppressWarnings("unchecked")
	public void apply() {
		// Track changes in aggregate manager, if given.
		if (aggregateMappingManager != null)
			aggregateMappingManager.updateAggregateMappings(mappings);

		// Pass to handler to notify of application of mappings has started.
		if (applicationHandler != null)
			try {
				applicationHandler.onPreApply(this);
			} catch (Throwable t) {
				logger.error("Mapping application handler failed on pre-application", t);
			}

		// Record mapping application jobs into a sorted set.
		// We want to apply some changes before others.
		SortedSet<ApplicationEntry> applicationEntries = new TreeSet<>();
		for (Map.Entry<String, String> entry : mappedClasses.entrySet()) {
			String preMappedName = entry.getKey();
			String postMappedName = entry.getValue();
			ClassPathNode preMappedPath = preMappingPaths.get(preMappedName);
			ClassPathNode postMappedPath = postMappingPaths.get(postMappedName);
			if (preMappedPath != null && postMappedPath != null) {
				applicationEntries.add(new ApplicationEntry(preMappedPath, postMappedPath, () -> {
					ClassBundle<ClassInfo> bundle = (ClassBundle<ClassInfo>) postMappedPath.getValueOfType(Bundle.class);
					if (bundle == null)
						throw new IllegalStateException("Cannot apply mapping for '" + preMappedName + "', path missing bundle");

					// Put mapped class into bundle
					ClassInfo postMappedClass = postMappedPath.getValue();
					bundle.put(postMappedClass);

					// Remove old classes if they have been renamed and do not occur
					// in a set of newly applied names
					if (!preMappedName.equals(postMappedName))
						bundle.remove(preMappedName);
				}));
			}
		}

		// Apply changes in sorted order.
		for (ApplicationEntry entry : applicationEntries)
			entry.applicationRunnable().run();

		// Log in console how many classes got mapped.
		logger.info("Applied mapping to {} classes", preMappingPaths.size());

		// Pass to handler again to notify of application of mappings has completed/
		if (applicationHandler != null)
			try {
				applicationHandler.onPostApply(this);
			} catch (Throwable t) {
				logger.error("Mapping application handler failed on post-application", t);
			}
	}

	/**
	 * @return The mappings implementation used in the operation.
	 */
	@Nonnull
	public Mappings getMappings() {
		return mappings;
	}

	/**
	 * @param preMappedName
	 * 		Pre-mapping name.
	 *
	 * @return {@code true} when the class was affected by the mapping operation.
	 */
	public boolean wasMapped(@Nonnull String preMappedName) {
		return mappedClasses.containsKey(preMappedName);
	}

	/**
	 * @param postMappingName
	 * 		Post-mapping name.
	 *
	 * @return Name of the class before the mapping operation.
	 * May be {@code null} if the post-mapping name was not renamed during the mapping operation.
	 */
	@Nullable
	public String getPreMappingName(@Nonnull String postMappingName) {
		return mappedClassesReverse.get(postMappingName);
	}

	/**
	 * @param preMappingName
	 * 		Pre-mapping name.
	 *
	 * @return Post-mapped class info.
	 * May be {@code null} if no the given pre-mapped name was not affected by the mapping operation.
	 */
	@Nullable
	public ClassInfo getPostMappingClass(@Nonnull String preMappingName) {
		ClassPathNode postMappingPath = getPostMappingPath(preMappingName);
		if (postMappingPath == null) return null;
		return postMappingPath.getValue();
	}

	/**
	 * @param preMappingName
	 * 		Pre-mapping name.
	 *
	 * @return Path node of post-mapped class.
	 * May be {@code null} if no the given pre-mapped name was not affected by the mapping operation.
	 */
	@Nullable
	public ClassPathNode getPostMappingPath(@Nonnull String preMappingName) {
		String postMappingName = mappedClasses.get(preMappingName);
		if (postMappingName == null) return null;
		return postMappingPaths.get(postMappingName);
	}

	/**
	 * @param postMappingName
	 * 		Post-mapping name.
	 *
	 * @return Pre-mapped class info.
	 * May be {@code null} if no the given post-mapped name was not present in the mapping operation output.
	 */
	@Nullable
	public ClassInfo getPreMappingClass(@Nonnull String postMappingName) {
		ClassPathNode preMappingPath = getPreMappingPath(postMappingName);
		if (preMappingPath == null) return null;
		return preMappingPath.getValue();
	}

	/**
	 * @param postMappingName
	 * 		Post-mapping name.
	 *
	 * @return Path node of pre-mapped class.
	 * May be {@code null} if no the given post-mapped name was not present in the mapping operation output.
	 */
	@Nullable
	public ClassPathNode getPreMappingPath(@Nonnull String postMappingName) {
		String preMappedName = getPreMappingName(postMappingName);
		return preMappedName == null ? null : preMappingPaths.get(preMappedName);
	}

	/**
	 * @return Stream of mapped path pairs.
	 * The {@link Pair#getLeft()} holds the pre-mapped path.
	 * The {@link Pair#getRight()} holds the post-mapped path, which may be {@code null} in some cases.
	 */
	@Nonnull
	public Stream<Pair<ClassPathNode, ClassPathNode>> streamPreToPostMappingPaths() {
		return getPreMappingPaths().values().stream()
				.map(p -> new Pair<>(p, getPostMappingPath(p.getValue().getName())));
	}

	/**
	 * @return Mapping of affected classes, to their new names.
	 * If a class was affected, but the name not changed, the key and value for that entry will be the same.
	 */
	@Nonnull
	public Map<String, String> getMappedClasses() {
		return mappedClasses;
	}

	/**
	 * @return Mapping of pre-mapped names to their path nodes.
	 */
	@Nonnull
	public Map<String, ClassPathNode> getPreMappingPaths() {
		return preMappingPaths;
	}

	/**
	 * @return Mapping of post-mapped names to their path nodes.
	 */
	@Nonnull
	public Map<String, ClassPathNode> getPostMappingPaths() {
		return postMappingPaths;
	}

	/**
	 * This class exists to facilitate sorting the order of which classes get updated in the workspace.
	 * The preferred order is:
	 * <ol>
	 *     <li>Classes with NEW names</li>
	 *     <li>Classes with LOW complexity</li>
	 * 	   <li>Anything else</li>
	 * </ol>
	 * The reason for NEW classes being first is so that existing classes, when updated, can see the NEW classes
	 * in the workspace. An example of this being important is in {@link AstService#newParser(JvmClassInfo)}.
	 * <br>
	 * Then LOW complexity classes are next. Statistically speaking it is typical for complex classes to rely on many
	 * more less complex classes. Following the principle of making new types available first, we make changes to the
	 * LOW complexity classes so that HIGH complexity classes can pull data from them.
	 *
	 * @param pre
	 * 		Pre mapped path.
	 * @param post
	 * 		Post mapped path.
	 * @param applicationRunnable
	 * 		Runnable that applies the mapping to the associated workspace.
	 */
	private record ApplicationEntry(@Nonnull ClassPathNode pre,
									@Nonnull ClassPathNode post,
									@Nonnull Runnable applicationRunnable) implements Comparable<ApplicationEntry> {
		/**
		 * @return {@code true} when pre-and-post mapping names are the same.
		 * Indicates the class was not mapped, but some references within it to others have been.
		 */
		private boolean isNameIdentity() {
			return pre.getValue().getName().equals(post.getValue().getName());
		}

		/**
		 * @return Rough level of complexity of the class in terms of how many types it references.
		 */
		public int complexity() {
			ClassInfo classInfo = post.getValue();
			if (classInfo.isJvmClass())
				return classInfo.asJvmClass().getReferencedClasses().size();
			return -1;
		}

		@Override
		public int compareTo(@Nonnull ApplicationEntry o) {
			boolean identity = isNameIdentity();
			boolean identityOther = o.isNameIdentity();

			// Entries with new names go first.
			if (identity && !identityOther)
				// Other class got renamed, we want it to go first.
				return 1;
			else if (!identity && identityOther)
				// We got renamed, we want to go first.
				return -1;

			// We want more complex classes to go last.
			int cmp = Integer.compare(complexity(), o.complexity());
			if (cmp != 0) return cmp;

			// Always want a unique ordering, so as a last resort we will compare by name.
			return post().getValue().getName().compareTo(o.post().getValue().getName());
		}
	}
}
