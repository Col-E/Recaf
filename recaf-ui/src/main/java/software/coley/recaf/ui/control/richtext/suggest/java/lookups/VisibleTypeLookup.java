package software.coley.recaf.ui.control.richtext.suggest.java.lookups;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletion;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionContext;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionFactory;
import software.coley.recaf.ui.control.richtext.suggest.java.JavaCompletionSession;
import software.coley.recaf.ui.control.richtext.suggest.java.TypeCandidate;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.TypeIndex;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ImportModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Lookup for types that are visible in the current completion context.
 *
 * @author Matt Coley
 */
public final class VisibleTypeLookup {
	/**
	 * Collects all types that are visible in the current completion context.
	 *
	 * @param session
	 * 		Current completion session.
	 *
	 * @return Map of visible types, keyed by qualified name.
	 *
	 * @see #isAccessibleType(TypeCandidate, String)
	 */
	@Nonnull
	public Map<String, TypeCandidate> collectVisibleTypes(@Nonnull JavaCompletionSession session) {
		TypeIndex typeIndex = session.typeIndex();
		Map<String, TypeCandidate> visibleTypes = new TreeMap<>();
		String currentPackage = session.currentCompilationPackageName();

		// First add the current class and its inner classes, if applicable.
		// These are always visible regardless of access modifiers.
		ClassPathNode currentPath = session.currentPath();
		if (currentPath != null) {
			ClassInfo currentClass = currentPath.getValue();
			addVisibleType(visibleTypes, typeIndex.findType(currentClass.getName().replace('/', '.').replace('$', '.')));
			for (InnerClassInfo innerClass : currentClass.getInnerClasses())
				addVisibleType(visibleTypes, typeIndex.findType(innerClass.getInnerClassName().replace('/', '.').replace('$', '.')));
		} else {
			JavaCompletionContext.DeclaredClassInfo declaredClassInfo = session.declaredClassInfo();
			if (declaredClassInfo != null) {
				String internalName = declaredClassInfo.internalName();
				addVisibleType(visibleTypes, new TypeCandidate(
						simpleName(internalName),
						internalName.replace('/', '.').replace('$', '.'),
						internalName,
						Objects.requireNonNullElse(declaredClassInfo.packageName(), "").replace('/', '.'),
						(declaredClassInfo.access() & Opcodes.ACC_ANNOTATION) != 0,
						declaredClassInfo.access(),
						null
				));
				for (TypeCandidate innerType : declaredClassInfo.innerTypes())
					addVisibleType(visibleTypes, innerType);
			}
		}

		// Next add types from the current package and imports. These are only visible if they are accessible.
		CompilationUnitModel unit = session.unit();
		if (unit != null) {
			String packageName = unit.getPackage().isDefaultPackage() ? "" : unit.getPackage().getName();
			addVisibleTypesFromPackage(session, visibleTypes, packageName.replace('/', '.'), currentPackage);
			for (ImportModel imp : unit.getImports()) {
				if (imp.isStatic())
					continue;
				String importName = imp.getName().replace('/', '.');
				if (importName.endsWith(".*")) {
					addVisibleTypesFromPackage(session, visibleTypes, importName.substring(0, importName.length() - 2), currentPackage);
				} else {
					addVisibleReferenceableType(visibleTypes, typeIndex.findType(importName), currentPackage);
				}
			}
		}

		// Add types from the java.lang package, which are always visible.
		addVisibleTypesFromPackage(session, visibleTypes, "java.lang", currentPackage);
		return visibleTypes;
	}

	/**
	 * Adds completions for all visible types matching the given prefix.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param completions
	 * 		Map to add completions to, keyed by completion name.
	 * @param partial
	 * 		Prefix to match against type simple names.
	 * @param annotationOnly
	 * 		Whether to only include annotation types.
	 * @param rank
	 * 		Base rank to use for completions, where lower is better.
	 */
	public void addVisibleTypeCompletions(@Nonnull JavaCompletionSession session,
	                                      @Nonnull Map<String, JavaCompletion> completions,
	                                      @Nonnull String partial,
	                                      boolean annotationOnly,
	                                      int rank) {
		for (TypeCandidate candidate : collectVisibleTypes(session).values()) {
			if (annotationOnly && !candidate.annotation())
				continue;
			if (!JavaCompletionFactory.matchesPrefix(candidate.simpleName(), partial))
				continue;
			JavaCompletion.addOrReplace(completions,
					JavaCompletionFactory.typeCompletion(candidate, rank + JavaCompletionFactory.prefixPenalty(candidate.simpleName(), partial)));
		}
	}

	/**
	 * Resolves a type reference in the current context, checking both qualified and simple names.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param receiverText
	 * 		Type reference text, which may be a qualified name or a simple name.
	 *
	 * @return Resolved type candidate, or {@code null} if no visible type matches the reference.
	 */
	@Nullable
	public TypeCandidate resolveVisibleTypeReference(@Nonnull JavaCompletionSession session, @Nonnull String receiverText) {
		// Check for an exact match first.
		TypeCandidate exactQualified = session.typeIndex().findType(receiverText);
		if (exactQualified != null)
			return exactQualified;

		// If no exact qualified match, check simple names among visible types.
		Collection<TypeCandidate> candidates = collectVisibleTypes(session).values();
		for (TypeCandidate candidate : candidates)
			if (candidate.simpleName().equals(receiverText))
				return candidate;

		return null;
	}

	/**
	 * @param candidate
	 * 		Type candidate to check for accessibility.
	 * @param currentPackage
	 * 		Current package name, or {@code null} if the current context is in the default package.
	 *
	 * @return {@code true} if the candidate is accessible in the current context, {@code false} otherwise.
	 */
	public static boolean isAccessibleType(@Nonnull TypeCandidate candidate, @Nullable String currentPackage) {
		if ((candidate.access() & Opcodes.ACC_PUBLIC) != 0)
			return true;
		return Objects.equals(candidate.packageName(), Objects.requireNonNullElse(currentPackage, ""));
	}

	/**
	 * Adds all types from the given package that are visible in the current context.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param visibleTypes
	 * 		Map to add visible types to, keyed by qualified name.
	 * @param packageName
	 * 		Dot-separated package name to add types from.
	 * @param currentPackage
	 * 		Current package name, or {@code null} if the current context is in the default package.
	 */
	private void addVisibleTypesFromPackage(@Nonnull JavaCompletionSession session,
	                                        @Nonnull Map<String, TypeCandidate> visibleTypes,
	                                        @Nonnull String packageName,
	                                        @Nullable String currentPackage) {
		for (TypeCandidate candidate : session.typeIndex().typesInPackage(packageName))
			addVisibleReferenceableType(visibleTypes, candidate, currentPackage);
		for (TypeCandidate candidate : runtimeTypesInPackage(session, packageName))
			addVisibleReferenceableType(visibleTypes, candidate, currentPackage);
	}

	/**
	 * @param visibleTypes
	 * 		Map to add the candidate to, keyed by qualified name.
	 * @param candidate
	 * 		Candidate to add, or {@code null} to ignore.
	 */
	private void addVisibleType(@Nonnull Map<String, TypeCandidate> visibleTypes, @Nullable TypeCandidate candidate) {
		if (candidate != null)
			visibleTypes.putIfAbsent(candidate.qualifiedName(), candidate);
	}

	/**
	 * @param visibleTypes
	 * 		Map to add the candidate to, keyed by qualified name.
	 * @param candidate
	 * 		Candidate to add, or {@code null} to ignore.
	 * @param currentPackage
	 * 		Current package name, or {@code null} if the current context is in the default package.
	 */
	private void addVisibleReferenceableType(@Nonnull Map<String, TypeCandidate> visibleTypes,
	                                         @Nullable TypeCandidate candidate,
	                                         @Nullable String currentPackage) {
		if (candidate != null && isAccessibleType(candidate, currentPackage))
			visibleTypes.putIfAbsent(candidate.qualifiedName(), candidate);
	}

	/**
	 * @param session
	 * 		Current completion session.
	 * @param packageName
	 * 		Dot-separated package name to search for types.
	 *
	 * @return Types from the given package that are present on the runtime classpath, which may not be indexed in the workspace.
	 */
	@Nonnull
	private List<TypeCandidate> runtimeTypesInPackage(@Nonnull JavaCompletionSession session, @Nonnull String packageName) {
		String packagePath = packageName.replace('.', '/');
		List<TypeCandidate> candidates = new ArrayList<>();
		addRuntimeTypesFromSet(session, candidates, ClasspathUtil.getSystemClassSet(), packagePath);
		addRuntimeTypesFromSet(session, candidates, ClasspathUtil.getClasspathClassSet(), packagePath);
		return candidates;
	}

	/**
	 * Adds types from the given set of class names that are in the specified package.
	 *
	 * @param session
	 * 		Current completion session.
	 * @param candidates
	 * 		List to add candidates to.
	 * @param classNames
	 * 		Set of class names to search, which should be sorted for efficient prefix searching.
	 * @param packagePath
	 * 		Internal package path name to match class names against.
	 */
	private void addRuntimeTypesFromSet(@Nonnull JavaCompletionSession session,
	                                    @Nonnull List<TypeCandidate> candidates,
	                                    @Nonnull NavigableSet<String> classNames,
	                                    @Nonnull String packagePath) {
		String prefix = packagePath.isEmpty() ? "" : packagePath + "/";
		for (String className : classNames.tailSet(prefix, true)) {
			// Skip if the class name doesn't start with the package path prefix, or if it has subpackages after the prefix.
			if (!prefix.isEmpty() && !className.startsWith(prefix))
				break;
			if (prefix.isEmpty() && className.indexOf('/') >= 0)
				break;

			// Skip if the class name has subpackages after the prefix, since those won't be in the current package.
			String relativeName = prefix.isEmpty() ? className : className.substring(prefix.length());
			if (relativeName.contains("/"))
				continue;

			// Try to find the class in the workspace first, since it may have more up-to-date information than the runtime classpath.
			ClassPathNode path = session.workspace().findClass(className);
			if (path == null) {
				RuntimeWorkspaceResource.getInstance().getJvmClassBundle().get(className);
				path = session.workspace().findClass(className);
			}
			if (path == null)
				continue;

			ClassInfo info = path.getValue();
			candidates.add(new TypeCandidate(
					simpleName(info.getName()),
					info.getName().replace('/', '.').replace('$', '.'),
					info.getName(),
					Objects.requireNonNullElse(info.getPackageName(), "").replace('/', '.'),
					(info.getAccess() & Opcodes.ACC_ANNOTATION) != 0,
					info.getAccess(),
					path
			));
		}
	}

	@Nonnull
	private static String simpleName(@Nonnull String internalName) {
		int slash = internalName.lastIndexOf('/');
		int dollar = internalName.lastIndexOf('$');
		int index = Math.max(slash, dollar);
		return index >= 0 ? internalName.substring(index + 1) : internalName;
	}
}
