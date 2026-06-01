package software.coley.recaf.ui.control.richtext.suggest.java.typeindex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.ui.control.richtext.suggest.java.TypeCandidate;
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
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Search-oriented index of workspace types used by Java completion.
 * <p>
 * The index stores the same class information in a few lookup shapes so completion code can
 * answer package, qualified-name, and visible-type queries without rescanning the workspace.
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
	private final Map<String, TypeCandidate> typesByQualifiedName = new HashMap<>();
	private final Map<String, IndexedEntry> activeEntriesByQualifiedName = new HashMap<>();
	private final Map<String, NavigableSet<IndexedEntry>> contributorsByQualifiedName = new HashMap<>();
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
	 * 		Dot-separated Java name.
	 *
	 * @return Indexed type for the name, or {@code null} when absent.
	 */
	@Nullable
	public TypeCandidate findType(@Nonnull String qualifiedName) {
		return typesByQualifiedName.get(qualifiedName);
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
		addEntry(new EntryKey(resourceOf(path), bundleOf(path), info.getName()), nextSequence++, createCandidate(path));
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

		NavigableSet<IndexedEntry> contributors = contributorsByQualifiedName.computeIfAbsent(candidate.qualifiedName(),
				ignored -> new TreeSet<>(ENTRY_ORDER));
		IndexedEntry previousWinner = activeEntriesByQualifiedName.get(candidate.qualifiedName());
		contributors.add(entry);
		refreshActiveWinner(candidate.qualifiedName(), previousWinner, contributors.first());
	}

	/**
	 * @param entry
	 * 		Entry to remove from the index.
	 */
	private void removeEntry(@Nonnull IndexedEntry entry) {
		String qualifiedName = entry.candidate().qualifiedName();
		NavigableSet<IndexedEntry> contributors = contributorsByQualifiedName.get(qualifiedName);
		if (contributors == null || !contributors.remove(entry))
			throw new IllegalStateException("Missing contributor set for " + qualifiedName);

		IndexedEntry previousWinner = activeEntriesByQualifiedName.get(qualifiedName);
		IndexedEntry currentWinner = contributors.isEmpty() ? null : contributors.first();
		if (contributors.isEmpty())
			contributorsByQualifiedName.remove(qualifiedName);
		refreshActiveWinner(qualifiedName, previousWinner, currentWinner);
	}

	/**
	 * Refreshes the active winner for a qualified name after a change to its contributors.
	 *
	 * @param qualifiedName
	 * 		Qualified name of the changed entry.
	 * @param previousWinner
	 * 		Prior active winner, or {@code null} if there was no winner.
	 * @param currentWinner
	 * 		New active winner, or {@code null} if there is no winner.
	 */
	private void refreshActiveWinner(@Nonnull String qualifiedName,
	                                 @Nullable IndexedEntry previousWinner,
	                                 @Nullable IndexedEntry currentWinner) {
		// Skip if they're the same.
		if (Objects.equals(previousWinner, currentWinner))
			return;

		// Remove the prior winner from active lookups, then add the new winner if there is one.
		if (previousWinner != null)
			removeActiveEntry(previousWinner);
		if (currentWinner != null)
			addActiveEntry(currentWinner);
		else
			typesByQualifiedName.remove(qualifiedName);
	}

	/**
	 * @param entry
	 * 		Entry to add to active lookups.
	 */
	private void addActiveEntry(@Nonnull IndexedEntry entry) {
		TypeCandidate candidate = entry.candidate();
		activeEntriesByQualifiedName.put(candidate.qualifiedName(), entry);
		typesByQualifiedName.put(candidate.qualifiedName(), candidate);
		activeTypesByPackage.computeIfAbsent(candidate.packageName(), ignored -> new TreeSet<>(ENTRY_ORDER)).add(entry);

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
		activeEntriesByQualifiedName.remove(candidate.qualifiedName());
		typesByQualifiedName.remove(candidate.qualifiedName());

		NavigableSet<IndexedEntry> packageEntries = activeTypesByPackage.get(candidate.packageName());
		if (packageEntries == null || !packageEntries.remove(entry))
			throw new IllegalStateException("Missing active package entry for " + candidate.qualifiedName());
		if (packageEntries.isEmpty())
			activeTypesByPackage.remove(candidate.packageName());

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

	private void incrementChildPackageRef(@Nonnull String parent, @Nonnull String child) {
		Map<String, Integer> counts = childPackageRefCounts.computeIfAbsent(parent, ignored -> new TreeMap<>());
		counts.merge(child, 1, Integer::sum);
		childPackages.computeIfAbsent(parent, ignored -> new TreeSet<>()).add(child);
	}

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
		return createCandidate(PathNodes.classPath(workspace, resource, bundle, info));
	}

	/**
	 * @param path
	 * 		Path to class to create a candidate for.
	 *
	 * @return Candidate for the class.
	 */
	@Nonnull
	private static TypeCandidate createCandidate(@Nonnull ClassPathNode path) {
		ClassInfo info = path.getValue();
		String packageName = Objects.requireNonNullElse(info.getPackageName(), "").replace('/', '.');
		String qualifiedName = info.getName().replace('/', '.').replace('$', '.');
		String simpleName = deriveSimpleName(info);
		return new TypeCandidate(
				simpleName,
				qualifiedName,
				info.getName(),
				packageName,
				(info.getAccess() & Opcodes.ACC_ANNOTATION) != 0,
				info.getAccess(),
				path
		);
	}

	private static void visitJvmClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		visitImmediateJvmClasses(resource, visitor);
		Queue<WorkspaceFileResource> embedded = new ArrayDeque<>(resource.getEmbeddedResources().values());
		while (!embedded.isEmpty()) {
			WorkspaceFileResource next = embedded.remove();
			visitImmediateJvmClasses(next, visitor);
			embedded.addAll(next.getEmbeddedResources().values());
		}
	}

	private static void visitAndroidClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		visitImmediateAndroidClasses(resource, visitor);
		Queue<WorkspaceFileResource> embedded = new ArrayDeque<>(resource.getEmbeddedResources().values());
		while (!embedded.isEmpty()) {
			WorkspaceFileResource next = embedded.remove();
			visitImmediateAndroidClasses(next, visitor);
			embedded.addAll(next.getEmbeddedResources().values());
		}
	}

	private static void visitImmediateJvmClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		List<JvmClassBundle> bundles = new ArrayList<>();
		bundles.addAll(resource.jvmClassBundleStream().toList());
		bundles.addAll(resource.versionedJvmClassBundleStream().toList());
		for (JvmClassBundle bundle : bundles) {
			for (ClassInfo info : bundle.values())
				visitor.visit(resource, bundle, info);
		}
	}

	private static void visitImmediateAndroidClasses(@Nonnull WorkspaceResource resource, @Nonnull ClassVisitor visitor) {
		for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
			for (ClassInfo info : bundle.values())
				visitor.visit(resource, bundle, info);
		}
	}

	@Nonnull
	private static WorkspaceResource resourceOf(@Nonnull ClassPathNode path) {
		return Objects.requireNonNull(path.getValueOfType(WorkspaceResource.class),
				"Class path missing workspace resource");
	}

	@Nonnull
	private static Bundle<?> bundleOf(@Nonnull ClassPathNode path) {
		return Objects.requireNonNull(path.getValueOfType(Bundle.class),
				"Class path missing bundle");
	}

	@Nonnull
	private static String deriveSimpleName(@Nonnull ClassInfo info) {
		// Nested classes should complete by their source-facing simple name rather than the full internal path.
		String name = info.getName();
		int lastSlash = name.lastIndexOf('/');
		int lastDollar = name.lastIndexOf('$');
		return name.substring(Math.max(lastSlash, lastDollar) + 1);
	}

	private interface ClassVisitor {
		void visit(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull ClassInfo info);
	}

	private record EntryKey(@Nonnull WorkspaceResource resource, @Nonnull Bundle<?> bundle, @Nonnull String className) {
		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof EntryKey entryKey))
				return false;
			return resource == entryKey.resource && bundle == entryKey.bundle && className.equals(entryKey.className);
		}

		@Override
		public int hashCode() {
			return Objects.hash(System.identityHashCode(resource), System.identityHashCode(bundle), className);
		}
	}

	private record IndexedEntry(@Nonnull EntryKey key, long sequence, @Nonnull TypeCandidate candidate) {}
}
