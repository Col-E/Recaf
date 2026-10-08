package software.coley.recaf.ui.control.richtext.suggest.typeindex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.ClassPathNode;

import java.util.Objects;

/**
 * Lightweight description of a type that can participate in completion lookups.
 *
 * @param simpleName
 * 		Display-friendly short name.
 * @param qualifiedName
 * 		Dot-separated name.
 * @param internalName
 * 		Internal class name.
 * @param packageName
 * 		Package containing the type.
 * @param annotation
 * 		Flag indicating the type is an annotation.
 * @param access
 * 		Raw access flags from the backing class.
 * @param path
 * 		Workspace path to the class, or {@code null} if the type is not backed by a workspace entry.
 *
 * @author Matt Coley
 */
public record TypeCandidate(@Nonnull String simpleName,
                            @Nonnull String qualifiedName,
                            @Nonnull String internalName,
                            @Nonnull String packageName,
                            boolean annotation,
                            int access,
                            @Nullable ClassPathNode path) {
	/**
	 * @param path
	 * 		Path to a class in the workspace.
	 *
	 * @return Candidate describing the class.
	 */
	@Nonnull
	public static TypeCandidate fromPath(@Nonnull ClassPathNode path) {
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

	@Nonnull
	private static String deriveSimpleName(@Nonnull ClassInfo info) {
		// Nested classes should complete by their source-facing simple name rather than the full internal path.
		String name = info.getName();
		int lastSlash = name.lastIndexOf('/');
		int lastDollar = name.lastIndexOf('$');
		return name.substring(Math.max(lastSlash, lastDollar) + 1);
	}
}
