package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndex;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeNameMatcher;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Workspace state shared by the assembler completion helpers for a single completion request.
 *
 * @param workspace
 * 		Workspace to resolve classes from.
 * @param types
 * 		Type index of the workspace.
 * @param graph
 * 		Inheritance graph used to find parent types.
 * @param className
 * 		Internal name of the class being edited, or {@code null} if there is no edited class.
 *
 * @author Matt Coley
 */
public record AssemblerCompletionScope(@Nonnull Workspace workspace,
                                       @Nonnull TypeIndex types,
                                       @Nonnull InheritanceGraph graph,
                                       @Nullable String className) {
	/**
	 * @return Dot-separated package of the edited class, or {@code null} if there is no edited class.
	 */
	@Nullable
	public String currentPackage() {
		return className == null ? null : packageOfInternalName(className);
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Dot-separated package of the class, or an empty string for the default package.
	 */
	@Nonnull
	public static String packageOfInternalName(@Nonnull String internalName) {
		int slash = internalName.lastIndexOf('/');
		return slash < 0 ? "" : internalName.substring(0, slash).replace('/', '.');
	}

	/**
	 * @param candidate
	 * 		Internal name of a class that may be a subclass.
	 * @param target
	 * 		Internal name of the class that may be a superclass.
	 *
	 * @return {@code true} when the candidate is the target, or has the target among its parents.
	 */
	public boolean isSameOrSubclass(@Nonnull String candidate, @Nonnull String target) {
		if (candidate.equals(target))
			return true;

		InheritanceVertex vertex = graph.getVertex(candidate);
		if (vertex == null)
			return false;

		for (InheritanceVertex parent : vertex.getAllParents())
			if (parent.getValue().getName().equals(target))
				return true;
		return false;
	}

	/**
	 * Finds types whose internal name starts with the given prefix.
	 * Workspace types win over runtime types with the same internal name.
	 * When the prefix is empty, only the workspace index is used.
	 *
	 * @param internalPrefix
	 * 		Prefix of internal names.
	 *
	 * @return Candidates whose internal name starts with the prefix.
	 */
	@Nonnull
	public List<TypeCandidate> typesWithPrefix(@Nonnull String internalPrefix) {
		Map<String, TypeCandidate> candidates = new LinkedHashMap<>();
		for (TypeCandidate candidate : types.typesWithInternalNamePrefix(internalPrefix))
			candidates.put(candidate.internalName(), candidate);
		if (internalPrefix.isEmpty())
			return List.copyOf(candidates.values());

		addRuntimeTypes(candidates, ClasspathUtil.getSystemClassSet(), internalPrefix);
		addRuntimeTypes(candidates, ClasspathUtil.getClasspathClassSet(), internalPrefix);
		return List.copyOf(candidates.values());
	}

	/**
	 * Finds types whose internal name or simple name starts with the given prefix.
	 * Workspace candidates win over runtime candidates with the same internal name.
	 * An empty prefix only returns workspace types, matching {@link #typesWithPrefix(String)}.
	 *
	 * @param prefix
	 * 		Prefix of an internal name or simple name.
	 *
	 * @return Matching candidates keyed by internal name in deterministic source order.
	 */
	@Nonnull
	public List<TypeMatch> typesMatchingPrefix(@Nonnull String prefix) {
		Map<String, TypeMatch> matches = new LinkedHashMap<>();
		for (TypeCandidate candidate : typesWithPrefix(prefix))
			addTypeMatch(matches, candidate, TypeMatchKind.INTERNAL_NAME_PREFIX);

		// If we see a slash, the user has specified the prefix text is at a minimum a package name, so we can skip simple-name matches.
		if (prefix.isEmpty() || prefix.indexOf('/') >= 0)
			return List.copyOf(matches.values());

		// No slash, we possibly have a simple-name match. Check the runtime classpath for any matches.
		for (TypeCandidate candidate : types.typesWithSimpleNamePrefix(prefix))
			addTypeMatch(matches, candidate, TypeMatchKind.SIMPLE_NAME_PREFIX);
		for (String className : RuntimeClassNameIndex.withSimpleNamePrefix(prefix))
			addRuntimeTypeMatch(matches, className, TypeMatchKind.SIMPLE_NAME_PREFIX);

		// Split the prefix into parts to check for camel-hump or acronym matches.
		List<String> queryParts = TypeNameMatcher.splitQuery(prefix);
		if (queryParts.size() >= 2) {
			TypeMatchKind kind = queryParts.stream().allMatch(part -> part.length() == 1)
					? TypeMatchKind.ACRONYM : TypeMatchKind.CAMEL_HUMP;
			for (TypeCandidate candidate : types.typesWithCamelHumpPrefix(prefix))
				addTypeMatch(matches, candidate, kind);
			for (String className : RuntimeClassNameIndex.withCamelHumpPrefix(prefix))
				addRuntimeTypeMatch(matches, className, kind);
		}

		return List.copyOf(matches.values());
	}

	/**
	 * Adds runtime types to the candidate list, skipping any that are already present.
	 *
	 * @param candidates
	 * 		Candidate map to add to.
	 * @param classNames
	 * 		Runtime class names to check for matches.
	 * @param internalPrefix
	 * 		Prefix of internal names to match against.
	 */
	private void addRuntimeTypes(@Nonnull Map<String, TypeCandidate> candidates,
	                             @Nonnull NavigableSet<String> classNames,
	                             @Nonnull String internalPrefix) {
		for (String className : classNames.tailSet(internalPrefix, true)) {
			// Skip classes that don't match the prefix, or are already in the candidate list (workspace wins over runtime).
			if (!className.startsWith(internalPrefix))
				break;
			addRuntimeType(candidates, className);
		}
	}

	/**
	 * Adds one runtime type to the candidate list, skipping types already supplied by the workspace.
	 *
	 * @param candidates
	 * 		Candidate map to add to.
	 * @param className
	 * 		Internal class name to resolve.
	 */
	private void addRuntimeType(@Nonnull Map<String, TypeCandidate> candidates, @Nonnull String className) {
		if (candidates.containsKey(className))
			return;

		TypeCandidate candidate = resolveRuntimeType(className);
		if (candidate != null)
			candidates.put(className, candidate);
	}

	/**
	 * @param className
	 * 		Internal name of the class to resolve.
	 *
	 * @return Candidate for the class, or {@code null} if it could not be resolved.
	 */
	@Nullable
	private TypeCandidate resolveRuntimeType(@Nonnull String className) {
		// Attempt to find the class in the workspace.
		// If it isn't there, then load it from the runtime classpath and try again.
		ClassPathNode path = workspace.findClass(className);
		if (path == null) {
			RuntimeWorkspaceResource.getInstance().getJvmClassBundle().get(className);
			path = workspace.findClass(className);
		}
		return path == null ? null : TypeCandidate.fromPath(path);
	}

	/**
	 * Adds a type match to the map, replacing any existing match with a lower specificity.
	 *
	 * @param matches
	 * 		Map of matches to add to.
	 * @param candidate
	 * 		Candidate that matched.
	 * @param kind
	 * 		Match category that was used to match the candidate.
	 */
	private static void addTypeMatch(@Nonnull Map<String, TypeMatch> matches,
	                                 @Nonnull TypeCandidate candidate,
	                                 @Nonnull TypeMatchKind kind) {
		TypeMatch existing = matches.get(candidate.internalName());
		if (existing == null || kind.rank() < existing.kind().rank())
			matches.put(candidate.internalName(), new TypeMatch(candidate, kind));
	}

	/**
	 * Adds a runtime type match to the map, skipping any that are already present.
	 *
	 * @param matches
	 * 		Map of matches to add to.
	 * @param className
	 * 		Internal name of the class to resolve.
	 * @param kind
	 * 		Match category that was used to match the candidate.
	 */
	private void addRuntimeTypeMatch(@Nonnull Map<String, TypeMatch> matches,
	                                 @Nonnull String className,
	                                 @Nonnull TypeMatchKind kind) {
		if (matches.containsKey(className))
			return;
		TypeCandidate candidate = resolveRuntimeType(className);
		if (candidate != null)
			addTypeMatch(matches, candidate, kind);
	}

	/**
	 * Match categories in descending specificity. Lower rank values are preferred.
	 */
	public enum TypeMatchKind {
		INTERNAL_NAME_PREFIX(0),
		SIMPLE_NAME_PREFIX(1),
		CAMEL_HUMP(2),
		ACRONYM(3);

		private final int rank;

		TypeMatchKind(int rank) {
			this.rank = rank;
		}

		public int rank() {
			return rank;
		}
	}

	/**
	 * Type candidate plus the quality tier by which it matched.
	 *
	 * @param candidate
	 * 		Matched type.
	 * @param kind
	 * 		Most-specific match category.
	 */
	public record TypeMatch(@Nonnull TypeCandidate candidate, @Nonnull TypeMatchKind kind) {}

	/**
	 * Lazily-built runtime name indexes. Candidate hump metadata is derived once from the running JDK and classpath,
	 * rather than recalculated for each completion lookup.
	 */
	private static final class RuntimeClassNameIndex {
		private static final RuntimeNameIndex INDEX = build();

		@Nonnull
		static List<String> withSimpleNamePrefix(@Nonnull String prefix) {
			return collectByPrefix(INDEX.bySimpleName(), prefix);
		}

		@Nonnull
		static List<String> withCamelHumpPrefix(@Nonnull String prefix) {
			List<String> queryParts = TypeNameMatcher.splitQuery(prefix);
			if (queryParts.size() < 2)
				return List.of();

			String initials = TypeNameMatcher.initials(queryParts);
			List<String> names = new ArrayList<>();
			for (Map.Entry<String, List<RuntimeTypeName>> entry : INDEX.byInitials().tailMap(initials, true).entrySet()) {
				if (!entry.getKey().startsWith(initials))
					break;
				for (RuntimeTypeName type : entry.getValue())
					if (type.name().matches(queryParts))
						names.add(type.internalName());
			}
			return names;
		}

		@Nonnull
		private static List<String> collectByPrefix(@Nonnull NavigableMap<String, List<RuntimeTypeName>> index,
		                                            @Nonnull String prefix) {
			List<String> names = new ArrayList<>();
			for (Map.Entry<String, List<RuntimeTypeName>> entry : index.tailMap(prefix, true).entrySet()) {
				if (!entry.getKey().startsWith(prefix))
					break;
				for (RuntimeTypeName type : entry.getValue())
					names.add(type.internalName());
			}
			return names;
		}

		@Nonnull
		private static RuntimeNameIndex build() {
			NavigableSet<String> classNames = new TreeSet<>(ClasspathUtil.getSystemClassSet());
			classNames.addAll(ClasspathUtil.getClasspathClassSet());
			Map<String, List<RuntimeTypeName>> bySimpleName = new TreeMap<>();
			Map<String, List<RuntimeTypeName>> byInitials = new TreeMap<>();
			for (String className : classNames) {
				int separator = Math.max(className.lastIndexOf('/'), className.lastIndexOf('$'));
				String simpleName = className.substring(separator + 1);
				TypeNameMatcher.IndexedName indexedName = TypeNameMatcher.index(simpleName);
				RuntimeTypeName type = new RuntimeTypeName(className, indexedName);
				bySimpleName.computeIfAbsent(simpleName, ignored -> new ArrayList<>()).add(type);
				byInitials.computeIfAbsent(indexedName.initials(), ignored -> new ArrayList<>()).add(type);
			}
			return new RuntimeNameIndex(freeze(bySimpleName), freeze(byInitials));
		}

		@Nonnull
		private static NavigableMap<String, List<RuntimeTypeName>> freeze(
				@Nonnull Map<String, List<RuntimeTypeName>> source) {
			NavigableMap<String, List<RuntimeTypeName>> result = new TreeMap<>();
			source.forEach((key, values) -> result.put(key, List.copyOf(values)));
			return result;
		}
	}

	private record RuntimeTypeName(@Nonnull String internalName,
	                               @Nonnull TypeNameMatcher.IndexedName name) {}

	private record RuntimeNameIndex(@Nonnull NavigableMap<String, List<RuntimeTypeName>> bySimpleName,
	                                @Nonnull NavigableMap<String, List<RuntimeTypeName>> byInitials) {}
}
