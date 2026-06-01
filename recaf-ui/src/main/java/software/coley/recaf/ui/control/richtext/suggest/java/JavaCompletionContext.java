package software.coley.recaf.ui.control.richtext.suggest.java;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.model.CompilationUnitModel;
import software.coley.sourcesolver.resolve.result.Resolution;

import java.util.List;

/**
 * Outline of a completion context for {@link JavaTabCompleter}.
 *
 * @author Matt Coley
 */
public interface JavaCompletionContext {
	/**
	 * @return Workspace backing completion lookups.
	 */
	@Nonnull
	Workspace getWorkspace();

	/**
	 * @return Current AST unit.
	 */
	@Nullable
	CompilationUnitModel getUnit();

	/**
	 * @return Current resolver.
	 */
	@Nullable
	ResolverAdapter getResolver();

	/**
	 * @param pos
	 * 		Offset in the current editor text.
	 *
	 * @return Offset mapped onto the last stable AST source.
	 */
	int mapCurrentPositionToAst(int pos);

	/**
	 * Resolves the current position against the last stable AST without notifying resolve listeners.
	 *
	 * @param pos
	 * 		Offset in the current editor text.
	 *
	 * @return Raw source-solver resolution of the content at the given position.
	 */
	@Nullable
	Resolution resolveRawPositionSilently(int pos);

	/**
	 * @return Backing class path when the editor is attached to a real workspace class, or {@code null}.
	 *
	 * @see #getDeclaredClassInfo()
	 */
	@Nullable
	ClassPathNode getPath();

	/**
	 * @return Metadata for the declared class when the editor is not backed by a real {@link ClassPathNode}.
	 *
	 * @see #getPath()
	 */
	@Nullable
	default DeclaredClassInfo getDeclaredClassInfo() {
		return null;
	}

	/**
	 * @return {@code true} when Java completion should be active for the current editor state.
	 */
	default boolean isCompletionAvailable() {
		return true;
	}

	/**
	 * Alternative class model / metadata for when the context does not wrap a workspace-backed class via {@link #getPath()}.
	 *
	 * @param internalName
	 * 		Internal class name.
	 * @param access
	 * 		Class access flags.
	 * @param fields
	 * 		Declared fields.
	 * @param methods
	 * 		Declared methods.
	 * @param innerTypes
	 * 		Direct inner classes visible by simple name.
	 *
	 * @see #getDeclaredClassInfo()
	 */
	record DeclaredClassInfo(@Nonnull String internalName,
	                         int access,
	                         @Nonnull List<FieldMember> fields,
	                         @Nonnull List<MethodMember> methods,
	                         @Nonnull List<TypeCandidate> innerTypes) {
		@Nullable
		public String packageName() {
			int slash = internalName.lastIndexOf('/');
			return slash > 0 ? internalName.substring(0, slash) : null;
		}
	}
}
