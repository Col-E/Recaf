package software.coley.recaf.ui.control.richtext.suggest.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndex;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

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
record AssemblerCompletionScope(@Nonnull Workspace workspace,
                                @Nonnull TypeIndex types,
                                @Nonnull InheritanceGraph graph,
                                @Nullable String className) {
	/**
	 * @return Dot-separated package of the edited class, or {@code null} if there is no edited class.
	 */
	@Nullable
	String currentPackage() {
		return className == null ? null : packageOfInternalName(className);
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Dot-separated package of the class, or an empty string for the default package.
	 */
	@Nonnull
	static String packageOfInternalName(@Nonnull String internalName) {
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
	boolean isSameOrSubclass(@Nonnull String candidate, @Nonnull String target) {
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
	List<TypeCandidate> typesWithPrefix(@Nonnull String internalPrefix) {
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
			if (candidates.containsKey(className))
				continue;

			// Attempt to find the class in the workspace.
			// If it isn't there, then load it from the runtime classpath and try again.
			ClassPathNode path = workspace.findClass(className);
			if (path == null) {
				RuntimeWorkspaceResource.getInstance().getJvmClassBundle().get(className);
				path = workspace.findClass(className);
			}
			if (path == null)
				continue;
			candidates.put(className, TypeCandidate.fromPath(path));
		}
	}
}
