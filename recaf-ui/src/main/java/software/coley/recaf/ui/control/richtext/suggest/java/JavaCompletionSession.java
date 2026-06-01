package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.JavaTypeIndexService;
import software.coley.recaf.ui.control.richtext.suggest.java.typeindex.TypeIndex;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.resolve.entry.ClassEntry;
import software.coley.sourcesolver.resolve.entry.EntryPool;

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
                                    @Nonnull JavaTypeIndexService typeIndexService,
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
}
