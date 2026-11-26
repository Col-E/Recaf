package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.lookups.CompletionMatching;
import software.coley.recaf.ui.control.richtext.suggest.lookups.MemberVisibility;
import software.coley.recaf.ui.control.richtext.suggest.lookups.TypeVisibility;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndex;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeIndexService;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.model.ImportModel;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;
import software.coley.sourcesolver.resolve.entry.FieldEntry;
import software.coley.sourcesolver.resolve.entry.MemberEntry;
import software.coley.sourcesolver.resolve.entry.MethodEntry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Session state for a single completion computation.
 *
 * @param completionContext
 * 		Completion context backing lookups.
 * @param typeIndexService
 * 		Type index service.
 * @param caretPosition
 * 		Current editor caret position.
 *
 * @author Matt Coley
 */
public record JavaCompletionSession(@Nonnull JavaCompletionContext completionContext,
                                    @Nonnull TypeIndexService typeIndexService,
                                    int caretPosition) {
	/**
	 * @return Workspace to use for lookups and resolutions.
	 */
	@Nonnull
	public Workspace workspace() {
		return completionContext.getWorkspace();
	}

	/**
	 * @return Compilation unit to use for lookups and resolutions.
	 * May be {@code null} if the caret is outside a compilation unit context.
	 */
	@Nullable
	public CompilationUnitModel unit() {
		return completionContext.getUnit();
	}

	/**
	 * @return Current class path node to use for lookups and resolutions.
	 * May be {@code null} if the caret is outside a class context.
	 *
	 * @see #declaredClassInfo() Alternative model when path is not used.
	 */
	@Nullable
	public ClassPathNode currentPath() {
		return completionContext.getPath();
	}

	/**
	 * @return Current class declaration model for lookups and resolutions.
	 * May be {@code null} if the caret is outside a class context or if the declaration model could not be resolved.
	 *
	 * @see #currentPath() Alternative model when declaration info is not used.
	 */
	@Nullable
	public JavaCompletionContext.DeclaredClassInfo declaredClassInfo() {
		return completionContext.getDeclaredClassInfo();
	}

	/**
	 * @return Type index to use for lookups and resolutions.
	 */
	@Nonnull
	public TypeIndex typeIndex() {
		// While this is a lookup into the index service, it should be cached by the service itself.
		return typeIndexService.getIndex(workspace());
	}

	/**
	 * @return Current package name derived from {@link #currentPath()} or {@link #declaredClassInfo()},
	 * or {@code null} if no class context is available.
	 */
	@Nullable
	public String currentPackageName() {
		ClassPathNode currentPath = currentPath();
		if (currentPath != null)
			return currentPath.getValue().getPackageName();
		JavaCompletionContext.DeclaredClassInfo declaredClassInfo = declaredClassInfo();
		return declaredClassInfo == null ? null : declaredClassInfo.packageName();
	}

	/**
	 * @return Current package name with dots instead of slashes,
	 * derived from {@link #unit()} first, falling back to {@link #currentPath()} or {@link #declaredClassInfo()}.
	 * Can be {@code null} if no compilation unit or class context is available.
	 */
	@Nullable
	public String currentCompilationPackageName() {
		CompilationUnitModel unit = unit();
		if (unit != null) {
			String packageName = unit.getPackage().isDefaultPackage() ? "" : unit.getPackage().getName();
			return packageName.replace('/', '.');
		}
		String currentPackage = currentPackageName();
		return currentPackage == null ? null : currentPackage.replace('/', '.');
	}

	/**
	 * @param pool
	 * 		Entry pool to use for lookups.
	 *
	 * @return Current class entry in the pool derived from {@link #currentPath()} or {@link #declaredClassInfo()},
	 * or {@code null} if no class context is available or if the entry could not be resolved.
	 */
	@Nullable
	public ClassEntry currentClassEntry(@Nonnull EntryPool pool) {
		ClassPathNode currentPath = currentPath();
		if (currentPath != null)
			return pool.getClass(currentPath.getValue().getName());
		JavaCompletionContext.DeclaredClassInfo declaredClassInfo = declaredClassInfo();
		if (declaredClassInfo != null)
			return pool.getClass(declaredClassInfo.internalName());
		return null;
	}

	/**
	 * Collects all types that are visible in the current completion context.
	 *
	 * @return Map of visible types, keyed by qualified name.
	 *
	 * @see TypeVisibility#isAccessibleType(TypeCandidate, String)
	 */
	@Nonnull
	public Map<String, TypeCandidate> visibleTypes() {
		TypeIndex typeIndex = typeIndex();
		Map<String, TypeCandidate> visibleTypes = new TreeMap<>();
		String currentPackage = currentCompilationPackageName();

		// First add the current class and its inner classes, if applicable.
		// These are always visible regardless of access modifiers.
		ClassPathNode currentPath = currentPath();
		if (currentPath != null) {
			ClassInfo currentClass = currentPath.getValue();
			addVisibleType(visibleTypes, typeIndex.findType(currentClass.getName().replace('/', '.').replace('$', '.')));
			for (InnerClassInfo innerClass : currentClass.getInnerClasses())
				addVisibleType(visibleTypes, typeIndex.findType(innerClass.getInnerClassName().replace('/', '.').replace('$', '.')));
		} else {
			JavaCompletionContext.DeclaredClassInfo declaredClassInfo = declaredClassInfo();
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
		CompilationUnitModel unit = unit();
		if (unit != null) {
			String packageName = unit.getPackage().isDefaultPackage() ? "" : unit.getPackage().getName();
			addVisibleTypesFromPackage(visibleTypes, packageName.replace('/', '.'), currentPackage);
			for (ImportModel imp : unit.getImports()) {
				if (imp.isStatic())
					continue;
				String importName = imp.getName().replace('/', '.');
				if (importName.endsWith(".*")) {
					addVisibleTypesFromPackage(visibleTypes, importName.substring(0, importName.length() - 2), currentPackage);
				} else {
					addVisibleReferenceableType(visibleTypes, typeIndex.findType(importName), currentPackage);
				}
			}
		}

		// Add types from the java.lang package, which are always visible.
		addVisibleTypesFromPackage(visibleTypes, "java.lang", currentPackage);
		return visibleTypes;
	}

	/**
	 * Resolves a type reference in the current context, checking both qualified and simple names.
	 *
	 * @param reference
	 * 		Type reference text, which may be a qualified name or a simple name.
	 *
	 * @return Resolved type candidate, or {@code null} if no visible type matches the reference.
	 */
	@Nullable
	public TypeCandidate resolveVisibleType(@Nonnull String reference) {
		// Check for an exact match first.
		TypeCandidate exactQualified = typeIndex().findType(reference);
		if (exactQualified != null)
			return exactQualified;

		// If no exact qualified match, check simple names among visible types.
		for (TypeCandidate candidate : visibleTypes().values())
			if (candidate.simpleName().equals(reference))
				return candidate;

		return null;
	}

	/**
	 * Adds all types from the given package that are visible in the current context.
	 *
	 * @param visibleTypes
	 * 		Map to add visible types to, keyed by qualified name.
	 * @param packageName
	 * 		Dot-separated package name to add types from.
	 * @param currentPackage
	 * 		Current package name, or {@code null} if the current context is in the default package.
	 */
	private void addVisibleTypesFromPackage(@Nonnull Map<String, TypeCandidate> visibleTypes,
	                                        @Nonnull String packageName,
	                                        @Nullable String currentPackage) {
		for (TypeCandidate candidate : typeIndex().typesInPackage(packageName))
			addVisibleReferenceableType(visibleTypes, candidate, currentPackage);
		for (TypeCandidate candidate : runtimeTypesInPackage(packageName))
			addVisibleReferenceableType(visibleTypes, candidate, currentPackage);
	}

	/**
	 * @param visibleTypes
	 * 		Map to add the candidate to, keyed by qualified name.
	 * @param candidate
	 * 		Candidate to add, or {@code null} to ignore.
	 */
	private static void addVisibleType(@Nonnull Map<String, TypeCandidate> visibleTypes, @Nullable TypeCandidate candidate) {
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
	private static void addVisibleReferenceableType(@Nonnull Map<String, TypeCandidate> visibleTypes,
	                                                @Nullable TypeCandidate candidate,
	                                                @Nullable String currentPackage) {
		if (candidate != null && TypeVisibility.isAccessibleType(candidate, currentPackage))
			visibleTypes.putIfAbsent(candidate.qualifiedName(), candidate);
	}

	/**
	 * @param packageName
	 * 		Dot-separated package name to search for types.
	 *
	 * @return Types from the given package that are present on the runtime classpath, which may not be indexed in the workspace.
	 */
	@Nonnull
	private List<TypeCandidate> runtimeTypesInPackage(@Nonnull String packageName) {
		String packagePath = packageName.replace('.', '/');
		List<TypeCandidate> candidates = new ArrayList<>();
		addRuntimeTypesFromSet(candidates, ClasspathUtil.getSystemClassSet(), packagePath);
		addRuntimeTypesFromSet(candidates, ClasspathUtil.getClasspathClassSet(), packagePath);
		return candidates;
	}

	/**
	 * Adds types from the given set of class names that are in the specified package.
	 *
	 * @param candidates
	 * 		List to add candidates to.
	 * @param classNames
	 * 		Set of class names to search, which should be sorted for efficient prefix searching.
	 * @param packagePath
	 * 		Internal package path name to match class names against.
	 */
	private void addRuntimeTypesFromSet(@Nonnull List<TypeCandidate> candidates,
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
			ClassPathNode path = workspace().findClass(className);
			if (path == null) {
				RuntimeWorkspaceResource.getInstance().getJvmClassBundle().get(className);
				path = workspace().findClass(className);
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

	/**
	 * Collect member completions for the given class and its super types.
	 *
	 * @param completions
	 * 		Map to add completions to. Completions will be added or replaced based on their name.
	 * @param classEntry
	 * 		Class to collect members from.
	 * @param currentClassEntry
	 * 		Class being edited, or {@code null} if no class context is available.
	 * @param partial
	 * 		Partial text to match member names against.
	 * @param methodsOnly
	 * 		{@code true} to only include methods, {@code false} to include fields as well.
	 * @param inheritedOnly
	 * 		{@code true} to only include inherited members, {@code false} to include declared members as well.
	 * @param currentPackage
	 * 		Current package name, used to determine visibility of package-private members.
	 * @param receiverMode
	 * 		Receiver mode to filter members by static/instance.
	 * @param insertMethodCall
	 * 		{@code true} to include method call syntax in method completions, {@code false} for simple method name.
	 * 		This is typically {@code false} when completing method references, and {@code true} for normal method calls.
	 */
	public void collectMemberCompletions(@Nonnull Map<String, JavaCompletion> completions,
	                                     @Nonnull ClassEntry classEntry,
	                                     @Nullable ClassEntry currentClassEntry,
	                                     @Nonnull String partial,
	                                     boolean methodsOnly,
	                                     boolean inheritedOnly,
	                                     @Nullable String currentPackage,
	                                     @Nonnull ReceiverMode receiverMode,
	                                     boolean insertMethodCall) {
		Set<String> visited = new HashSet<>();
		collectMemberCompletions(completions, classEntry, partial, methodsOnly, inheritedOnly, currentPackage,
				receiverMode, insertMethodCall, classEntry, currentClassEntry, visited, 0);
	}

	private void collectMemberCompletions(@Nonnull Map<String, JavaCompletion> completions,
	                                      @Nonnull ClassEntry classEntry,
	                                      @Nonnull String partial,
	                                      boolean methodsOnly,
	                                      boolean inheritedOnly,
	                                      @Nullable String currentPackage,
	                                      @Nonnull ReceiverMode receiverMode,
	                                      boolean insertMethodCall,
	                                      @Nonnull ClassEntry receiverClassEntry,
	                                      @Nullable ClassEntry currentClassEntry,
	                                      @Nonnull Set<String> visited,
	                                      int depth) {
		if (!visited.add(classEntry.getName()))
			return;

		boolean declared = depth == 0 && !inheritedOnly;
		ClassPathNode ownerPath = workspace().findClass(classEntry.getName());

		if (!methodsOnly) {
			for (FieldEntry field : classEntry.getDeclaredFields()) {
				if (!matchesReceiverMode(field, receiverMode))
					continue;
				if (!isVisibleMember(field, classEntry, receiverClassEntry, currentClassEntry, currentPackage))
					continue;
				String name = field.getName();
				if (!CompletionMatching.matchesPrefix(name, partial))
					continue;
				ClassMemberPathNode memberPath = ownerPath == null ? null : ownerPath.child(name, field.getDescriptor());
				JavaCompletion.addOrReplace(completions, new JavaCompletion(
						CompletionKind.FIELD,
						JavaCompletionFactory.displayField(name, field.getDescriptor()),
						name,
						(declared ? 0 : 10) + CompletionMatching.prefixPenalty(name, partial),
						memberPath,
						name,
						0,
						""
				));
			}
		}

		for (MethodEntry method : classEntry.getDeclaredMethods()) {
			String name = method.getName();
			if (name.startsWith("<"))
				continue;
			if (!matchesReceiverMode(method, receiverMode))
				continue;
			if (!isVisibleMember(method, classEntry, receiverClassEntry, currentClassEntry, currentPackage))
				continue;
			if (!CompletionMatching.matchesPrefix(name, partial))
				continue;
			ClassMemberPathNode memberPath = ownerPath == null ? null : ownerPath.child(name, method.getDescriptor());
			JavaCompletion.addOrReplace(completions, JavaCompletionFactory.methodCompletion(
					name,
					method.getDescriptor(),
					(declared ? 1 : 11) + CompletionMatching.prefixPenalty(name, partial),
					memberPath,
					insertMethodCall
			));
		}

		ClassEntry superEntry = classEntry.getSuperEntry();
		if (superEntry != null)
			collectMemberCompletions(completions, superEntry, partial, methodsOnly, false, currentPackage,
					receiverMode, insertMethodCall, receiverClassEntry, currentClassEntry, visited, depth + 1);
		for (ClassEntry implementedEntry : classEntry.getImplementedEntries())
			collectMemberCompletions(completions, implementedEntry, partial, methodsOnly, false, currentPackage,
					receiverMode, insertMethodCall, receiverClassEntry, currentClassEntry, visited, depth + 1);
	}

	/**
	 * Find a visible field with the given name in the class or its super types.
	 *
	 * @param classEntry
	 * 		Class to start the search from.
	 * @param currentClassEntry
	 * 		Class being edited, or {@code null} if no class context is available.
	 * @param fieldName
	 * 		Name of the field to find.
	 * @param currentPackage
	 * 		Current package name, used to determine visibility of package-private members.
	 * @param receiverMode
	 * 		Receiver mode to filter members by static/instance.
	 *
	 * @return A visible field entry with the given name, or {@code null} if no such field is found.
	 */
	@Nullable
	public static FieldEntry findVisibleField(@Nonnull ClassEntry classEntry,
	                                          @Nullable ClassEntry currentClassEntry,
	                                          @Nonnull String fieldName,
	                                          @Nullable String currentPackage,
	                                          @Nonnull ReceiverMode receiverMode) {
		return findVisibleField(classEntry, classEntry, currentClassEntry, fieldName, currentPackage,
				receiverMode, new HashSet<>());
	}

	@Nullable
	private static FieldEntry findVisibleField(@Nonnull ClassEntry ownerClassEntry,
	                                           @Nonnull ClassEntry receiverClassEntry,
	                                           @Nullable ClassEntry currentClassEntry,
	                                           @Nonnull String fieldName,
	                                           @Nullable String currentPackage,
	                                           @Nonnull ReceiverMode receiverMode,
	                                           @Nonnull Set<String> visited) {
		// Skip if we've already visited this class, to prevent infinite loops in case of junk classes with circular inheritance.
		if (!visited.add(ownerClassEntry.getName()))
			return null;

		// Check for any matching declared fields in this class.
		for (FieldEntry field : ownerClassEntry.getDeclaredFields()) {
			if (!fieldName.equals(field.getName()))
				continue;
			if (!matchesReceiverMode(field, receiverMode))
				continue;
			if (isVisibleMember(field, ownerClassEntry, receiverClassEntry, currentClassEntry, currentPackage))
				return field;
		}

		// If not found, check super types.
		ClassEntry superEntry = ownerClassEntry.getSuperEntry();
		if (superEntry != null) {
			FieldEntry inherited = findVisibleField(superEntry, receiverClassEntry, currentClassEntry, fieldName,
					currentPackage, receiverMode, visited);
			if (inherited != null)
				return inherited;
		}
		for (ClassEntry implementedEntry : ownerClassEntry.getImplementedEntries()) {
			FieldEntry inherited = findVisibleField(implementedEntry, receiverClassEntry, currentClassEntry, fieldName,
					currentPackage, receiverMode, visited);
			if (inherited != null)
				return inherited;
		}

		return null;
	}

	private static boolean matchesReceiverMode(@Nonnull MemberEntry member, @Nonnull ReceiverMode receiverMode) {
		return switch (receiverMode) {
			case ANY -> true;
			case STATIC_ONLY -> member.isStatic();
			case INSTANCE_ONLY -> !member.isStatic();
		};
	}

	private static boolean isVisibleMember(@Nonnull MemberEntry member,
	                                       @Nonnull ClassEntry ownerClassEntry,
	                                       @Nonnull ClassEntry receiverClassEntry,
	                                       @Nullable ClassEntry currentClassEntry,
	                                       @Nullable String currentPackage) {
		// Determine the access level of the member.
		MemberVisibility.Access access;
		if (member.isPublic())
			access = MemberVisibility.Access.PUBLIC;
		else if (member.isProtected())
			access = MemberVisibility.Access.PROTECTED;
		else if (member.isPrivate())
			access = MemberVisibility.Access.PRIVATE;
		else
			access = MemberVisibility.Access.PACKAGE;
		boolean declaredInCurrentClass = currentClassEntry != null && ownerClassEntry.getName().equals(currentClassEntry.getName());
		boolean samePackage = Objects.equals(currentPackage, ownerClassEntry.getPackageName());

		// Protected members are visible to subclasses, but only if the receiver type is a subclass of the current class.
		return MemberVisibility.isVisible(access, declaredInCurrentClass, samePackage,
				() -> currentClassEntry != null &&
						isSameOrSubclass(currentClassEntry, ownerClassEntry) &&
						isSameOrSubclass(receiverClassEntry, currentClassEntry));
	}

	private static boolean isSameOrSubclass(@Nonnull ClassEntry candidate, @Nonnull ClassEntry target) {
		return isSameOrSubclass(candidate, target, new HashSet<>());
	}

	private static boolean isSameOrSubclass(@Nonnull ClassEntry candidate,
	                                        @Nonnull ClassEntry target,
	                                        @Nonnull Set<String> visited) {
		// Skip if visited.
		if (!visited.add(candidate.getName()))
			return false;

		// Check for match.
		if (candidate.getName().equals(target.getName()))
			return true;

		// Check super types.
		ClassEntry superEntry = candidate.getSuperEntry();
		if (superEntry != null && isSameOrSubclass(superEntry, target, visited))
			return true;
		for (ClassEntry implementedEntry : candidate.getImplementedEntries())
			if (isSameOrSubclass(implementedEntry, target, visited))
				return true;
		return false;
	}
}
