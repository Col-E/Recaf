package software.coley.recaf.ui.control.richtext.suggest.typeindex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Search-oriented index of workspace types used by tab completion.
 *
 * @author Matt Coley
 */
public final class TypeIndex {
	private static final Comparator<IndexedEntry> ENTRY_ORDER = Comparator
			.comparingLong(IndexedEntry::sequence)
			.thenComparingInt(entry -> System.identityHashCode(entry.key().resource()))
			.thenComparingInt(entry -> System.identityHashCode(entry.key().bundle()))
			.thenComparing(entry -> entry.key().className());
	private final Workspace workspace;
	private final Map<String, NavigableSet<IndexedEntry>> contributorsByInternalName = new HashMap<>();
	private final NavigableMap<String, IndexedEntry> activeEntriesByInternalName = new TreeMap<>();
	private final Map<String, NavigableSet<IndexedEntry>> activeWinnersByQualifiedName = new HashMap<>();
	private final Map<String, NavigableSet<IndexedEntry>> activeTypesByPackage = new HashMap<>();
	private final Map<String, Integer> activeTypeCountsByPackage = new HashMap<>();
	private final Map<String, Map<String, Integer>> childPackageRefCounts = new HashMap<>();
	private final Map<String, Set<String>> childPackages = new HashMap<>();
	private final Map<EntryKey, IndexedEntry> entriesByKey = new HashMap<>();
	private long nextSequence;

	private TypeIndex(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Builds an index over all classes currently visible in the workspace.
	 *
	 * @param workspace
	 * 		Workspace to index.
	 *
	 * @return Populated type index.
	 */
	@Nonnull
	public static TypeIndex build(@Nonnull Workspace workspace) {
		TypeIndex index = new TypeIndex(workspace);
		workspace.classesStream().forEach(index::register);
		return index;
	}

	/**
	 * Adds a class to the index.
	 *
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		Class to add.
	 */
	void addClass(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull ClassInfo info) {
		EntryKey key = new EntryKey(resource, bundle, info.getName());
		if (entriesByKey.containsKey(key))
			throw new IllegalStateException("Class already indexed: " + info.getName());
		addEntry(key, nextSequence++, createCandidate(resource, bundle, info));
	}

	/**
	 * Updates a class in the index while preserving its relative ordering.
	 *
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param oldInfo
	 * 		Previous class value.
	 * @param newInfo
	 * 		New class value.
	 */
	void updateClass(@Nonnull WorkspaceResource resource,
	                 @Nonnull Bundle<?> bundle,
	                 @Nonnull ClassInfo oldInfo,
	                 @Nonnull ClassInfo newInfo) {
		IndexedEntry existing = entriesByKey.remove(new EntryKey(resource, bundle, oldInfo.getName()));
		if (existing == null)
			throw new IllegalStateException("Missing indexed class for update: " + oldInfo.getName());
		removeEntry(existing);
		addEntry(new EntryKey(resource, bundle, newInfo.getName()), existing.sequence(), createCandidate(resource, bundle, newInfo));
	}

	/**
	 * Removes a class from the index.
	 *
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param info
	 * 		Class to remove.
	 */
	void removeClass(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull ClassInfo info) {
		IndexedEntry existing = entriesByKey.remove(new EntryKey(resource, bundle, info.getName()));
		if (existing == null)
			throw new IllegalStateException("Missing indexed class for removal: " + info.getName());
		removeEntry(existing);
	}

	/**
	 * Adds all classes from the resource in the same order used by the initial build.
	 *
	 * @param resource
	 * 		Resource to add.
	 */
	void addResource(@Nonnull WorkspaceResource resource) {
		visitJvmClasses(resource, this::addClass);
		visitAndroidClasses(resource, this::addClass);
	}

	/**
	 * Removes all classes from the resource in the same order used by the initial build.
	 *
	 * @param resource
	 * 		Resource to remove.
	 */
	void removeResource(@Nonnull WorkspaceResource resource) {
		visitJvmClasses(resource, this::removeClass);
		visitAndroidClasses(resource, this::removeClass);
	}

	/**
	 * @param qualifiedName
	 * 		Dot-separated name.
	 *
	 * @return Indexed type for the name, or {@code null} when absent. When several active types share the name,
	 * the one with the lowest sequence is returned.
	 */
	@Nullable
	public TypeCandidate findType(@Nonnull String qualifiedName) {
		NavigableSet<IndexedEntry> winners = activeWinnersByQualifiedName.get(qualifiedName);
		return winners == null ? null : winners.first().candidate();
	}

	/**
	 * @param prefix
	 * 		Prefix of internal names, such as {@code java/util/Ar}. An empty prefix matches every type.
	 *
	 * @return Active types whose internal name starts with the prefix, ordered by internal name.
	 */
	@Nonnull
	public List<TypeCandidate> typesWithInternalNamePrefix(@Nonnull String prefix) {
		List<TypeCandidate> candidates = new ArrayList<>();
		for (Map.Entry<String, IndexedEntry> entry : activeEntriesByInternalName.tailMap(prefix, true).entrySet()) {
			if (!entry.getKey().startsWith(prefix))
				break;
			candidates.add(entry.getValue().candidate());
		}
		return List.copyOf(candidates);
	}

	/**
	 * @param packageName
	 * 		Dot-separated package name.
	 *
	 * @return Types directly contained in the package.
	 */
	@Nonnull
	public List<TypeCandidate> typesInPackage(@Nonnull String packageName) {
		NavigableSet<IndexedEntry> entries = activeTypesByPackage.get(packageName);
		if (entries == null || entries.isEmpty())
			return List.of();
		List<TypeCandidate> candidates = new ArrayList<>(entries.size());
		for (IndexedEntry entry : entries)
			candidates.add(entry.candidate());
		return List.copyOf(candidates);
	}

	/**
	 * @param packageName
	 * 		Dot-separated package name.
	 *
	 * @return Known child packages below the given package.
	 */
	@Nonnull
	public Collection<String> childPackages(@Nonnull String packageName) {
		Set<String> packages = childPackages.get(packageName);
		return packages == null ? Set.of() : Set.copyOf(packages);
	}

	/**
	 * @param path
	 * 		Path to class to register.
	 */
	private void register(@Nonnull ClassPathNode path) {
		ClassInfo info = path.getValue();
		addEntry(new EntryKey(resourceOf(path), bundleOf(path), info.getName()), nextSequence++, TypeCandidate.fromPath(path));
	}

	/**
	 * @param key
	 * 		Unique key for the entry.
	 * @param sequence
	 * 		Sequence number for the entry, used to preserve relative ordering.
	 * @param candidate
	 * 		Type candidate represented by the entry.
	 */
	private void addEntry(@Nonnull EntryKey key, long sequence, @Nonnull TypeCandidate candidate) {
		IndexedEntry entry = new IndexedEntry(key, sequence, candidate);
		entriesByKey.put(key, entry);

		String internalName = candidate.internalName();
		NavigableSet<IndexedEntry> contributors = contributorsByInternalName.computeIfAbsent(internalName,
				ignored -> new TreeSet<>(ENTRY_ORDER));
		IndexedEntry previousWinner = activeEntriesByInternalName.get(internalName);
		contributors.add(entry);
		refreshActiveWinner(previousWinner, contributors.first());
	}

	/**
	 * @param entry
	 * 		Entry to remove from the index.
	 */
	private void removeEntry(@Nonnull IndexedEntry entry) {
		String internalName = entry.candidate().internalName();
		NavigableSet<IndexedEntry> contributors = contributorsByInternalName.get(internalName);
		if (contributors == null || !contributors.remove(entry))
			throw new IllegalStateException("Missing contributor set for " + internalName);

		IndexedEntry previousWinner = activeEntriesByInternalName.get(internalName);
		IndexedEntry currentWinner = contributors.isEmpty() ? null : contributors.first();
		if (contributors.isEmpty())
			contributorsByInternalName.remove(internalName);
		refreshActiveWinner(previousWinner, currentWinner);
	}

	/**
	 * Refreshes the active winner for an internal name after a change to its contributors.
	 *
	 * @param previousWinner
	 * 		Prior active winner, or {@code null} if there was no winner.
	 * @param currentWinner
	 * 		New active winner, or {@code null} if there is no winner.
	 */
	private void refreshActiveWinner(@Nullable IndexedEntry previousWinner, @Nullable IndexedEntry currentWinner) {
		// Skip if they're the same.
		if (Objects.equals(previousWinner, currentWinner))
			return;

		// Remove the prior winner from active lookups, then add the new winner if there is one.
		if (previousWinner != null)
			removeActiveEntry(previousWinner);
		if (currentWinner != null)
			addActiveEntry(currentWinner);
	}

	/**
	 * @param entry
	 * 		Entry to add to active lookups.
	 */
	private void addActiveEntry(@Nonnull IndexedEntry entry) {
		TypeCandidate candidate = entry.candidate();
		activeEntriesByInternalName.put(candidate.internalName(), entry);
		activeWinnersByQualifiedName.computeIfAbsent(candidate.qualifiedName(), ignored -> new TreeSet<>(ENTRY_ORDER)).add(entry);
		activeTypesByPackage.computeIfAbsent(candidate.packageName(), ignored -> new TreeSet<>(ENTRY_ORDER)).add(entry);

		// Update the package count, and register the package hierarchy if this is the first type in the package.
		int priorCount = activeTypeCountsByPackage.getOrDefault(candidate.packageName(), 0);
		activeTypeCountsByPackage.put(candidate.packageName(), priorCount + 1);
		if (priorCount == 0)
			registerPackageHierarchy(candidate.packageName());
	}

	/**
	 * @param entry
	 * 		Entry to remove from active lookups.
	 */
	private void removeActiveEntry(@Nonnull IndexedEntry entry) {
		TypeCandidate candidate = entry.candidate();
		activeEntriesByInternalName.remove(candidate.internalName());

		// Remove the entry from the qualified name and package lookups.
		// If the sets are empty after removal, remove the set entirely.
		NavigableSet<IndexedEntry> winners = activeWinnersByQualifiedName.get(candidate.qualifiedName());
		if (winners == null || !winners.remove(entry))
			throw new IllegalStateException("Missing active qualified name entry for " + candidate.internalName());
		if (winners.isEmpty())
			activeWinnersByQualifiedName.remove(candidate.qualifiedName());

		// Same idea with package entries.
		NavigableSet<IndexedEntry> packageEntries = activeTypesByPackage.get(candidate.packageName());
		if (packageEntries == null || !packageEntries.remove(entry))
			throw new IllegalStateException("Missing active package entry for " + candidate.internalName());
		if (packageEntries.isEmpty())
			activeTypesByPackage.remove(candidate.packageName());

		// And with the package counts.
		int updatedCount = activeTypeCountsByPackage.getOrDefault(candidate.packageName(), 0) - 1;
		if (updatedCount < 0)
			throw new IllegalStateException("Negative package count for " + candidate.packageName());
		if (updatedCount == 0) {
			activeTypeCountsByPackage.remove(candidate.packageName());
			unregisterPackageHierarchy(candidate.packageName());
		} else {
			activeTypeCountsByPackage.put(candidate.packageName(), updatedCount);
		}
	}

	/**
	 * @param packageName
	 * 		Package name to register in the hierarchy, along with all parent packages.
	 */
	private void registerPackageHierarchy(@Nonnull String packageName) {
		if (packageName.isEmpty())
			return;

		String parent = "";
		for (String segment : packageName.split("\\.")) {
			String child = parent.isEmpty() ? segment : parent + '.' + segment;
			incrementChildPackageRef(parent, child);
			parent = child;
		}
	}

	/**
	 * @param packageName
	 * 		Package name to unregister from the hierarchy, along with all parent packages.
	 */
	private void unregisterPackageHierarchy(@Nonnull String packageName) {
		if (packageName.isEmpty())
			return;

		String parent = "";
		for (String segment : packageName.split("\\.")) {
			String child = parent.isEmpty() ? segment : parent + '.' + segment;
			decrementChildPackageRef(parent, child);
			parent = child;
		}
	}

	/**
	 * Add a reference from a parent package to a child package.
	 *
	 * @param parent
	 * 		Parent package name.
	 * @param child
	 * 		Child package name.
	 */
	private void incrementChildPackageRef(@Nonnull String parent, @Nonnull String child) {
		Map<String, Integer> counts = childPackageRefCounts.computeIfAbsent(parent, ignored -> new TreeMap<>());
		counts.merge(child, 1, Integer::sum);
		childPackages.computeIfAbsent(parent, ignored -> new TreeSet<>()).add(child);
	}

	/**
	 * Remove a reference from a parent package to a child package.
	 *
	 * @param parent
	 * 		Parent package name.
	 * @param child
	 * 		Child package name.
	 */
	private void decrementChildPackageRef(@Nonnull String parent, @Nonnull String child) {
		Map<String, Integer> counts = childPackageRefCounts.get(parent);
		if (counts == null)
			throw new IllegalStateException("Missing child package parent: " + parent);

		Integer current = counts.get(child);
		if (current == null)
			throw new IllegalStateException("Missing child package edge: " + parent + " -> " + child);

		if (current == 1) {
			counts.remove(child);
			Set<String> children = childPackages.get(parent);
			if (children != null) {
				children.remove(child);
				if (children.isEmpty())
					childPackages.remove(parent);
			}
			if (counts.isEmpty())
				childPackageRefCounts.remove(parent);
		} else {
			counts.put(child, current - 1);
		}
	}

	/**
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param info
	 * 		Class to create a candidate for.
	 *
	 * @return Candidate for the class.
	 */
	@Nonnull
	private TypeCandidate createCandidate(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull ClassInfo info) {
		return TypeCandidate.fromPath(PathNodes.classPath(workspace, resource, bundle, info));
	}

	/**
	 * Visits all classes in the resource and its embedded resources.
	 *
	 * @param resource
	 * 		Resource to visit.
	 * @param visitor
	 * 		Visitor to call for each class.
	 */
	private static void visitJvmClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		visitImmediateJvmClasses(resource, visitor);
		Queue<WorkspaceFileResource> embedded = new ArrayDeque<>(resource.getEmbeddedResources().values());
		while (!embedded.isEmpty()) {
			WorkspaceFileResource next = embedded.remove();
			visitImmediateJvmClasses(next, visitor);
			embedded.addAll(next.getEmbeddedResources().values());
		}
	}

	/**
	 * Visits all classes in the resource and its embedded resources.
	 *
	 * @param resource
	 * 		Resource to visit.
	 * @param visitor
	 * 		Visitor to call for each class.
	 */
	private static void visitAndroidClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		visitImmediateAndroidClasses(resource, visitor);
		Queue<WorkspaceFileResource> embedded = new ArrayDeque<>(resource.getEmbeddedResources().values());
		while (!embedded.isEmpty()) {
			WorkspaceFileResource next = embedded.remove();
			visitImmediateAndroidClasses(next, visitor);
			embedded.addAll(next.getEmbeddedResources().values());
		}
	}

	/**
	 * Visits all classes in the resource, but not its embedded resources.
	 *
	 * @param resource
	 * 		Resource to visit.
	 * @param visitor
	 * 		Visitor to call for each class.
	 */
	private static void visitImmediateJvmClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		List<JvmClassBundle> bundles = new ArrayList<>();
		bundles.addAll(resource.jvmClassBundleStream().toList());
		bundles.addAll(resource.versionedJvmClassBundleStream().toList());
		for (JvmClassBundle bundle : bundles) {
			for (ClassInfo info : bundle.values())
				visitor.visit(resource, bundle, info);
		}
	}

	/**
	 * Visits all classes in the resource, but not its embedded resources.
	 *
	 * @param resource
	 * 		Resource to visit.
	 * @param visitor
	 * 		Visitor to call for each class.
	 */
	private static void visitImmediateAndroidClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
			for (ClassInfo info : bundle.values())
				visitor.visit(resource, bundle, info);
		}
	}

	@Nonnull
	private static WorkspaceResource resourceOf(@Nonnull ClassPathNode path) {
		return Objects.requireNonNull(path.getValueOfType(WorkspaceResource.class), "Class path missing workspace resource");
	}

	@Nonnull
	private static Bundle<?> bundleOf(@Nonnull ClassPathNode path) {
		return Objects.requireNonNull(path.getValueOfType(Bundle.class), "Class path missing bundle");
	}

	/**
	 * Visitor for classes in a resource.
	 */
	private interface ClassVisitor {
		/**
		 * @param resource
		 * 		Resource containing the class.
		 * @param bundle
		 * 		Bundle containing the class.
		 * @param info
		 * 		Class to visit.
		 */
		void visit(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull ClassInfo info);
	}

	/**
	 * Unique key for an entry in the index.
	 *
	 * @param resource
	 * 		Resource containing the class.
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param className
	 * 		Class name, in internal form.
	 */
	private record EntryKey(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull String className) {
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof EntryKey(WorkspaceResource otherResource, Bundle<?> otherBundle, String otherName)))
				return false;
			return resource == otherResource && bundle == otherBundle && className.equals(otherName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(System.identityHashCode(resource), System.identityHashCode(bundle), className);
		}
	}

	/**
	 * Entry in the index.
	 *
	 * @param key
	 * 		Unique key for the entry.
	 * @param sequence
	 * 		Sequence number for the entry, used to preserve relative ordering.
	 * @param candidate
	 * 		Type candidate represented by the entry.
	 */
	private record IndexedEntry(@Nonnull EntryKey key, long sequence, @Nonnull TypeCandidate candidate) {}
}
