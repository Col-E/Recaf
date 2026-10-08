package software.coley.recaf.ui.control.richtext.suggest.lookups;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.ui.control.richtext.suggest.typeindex.TypeCandidate;

import java.util.Objects;

/**
 * Type accessibility checks shared by completion languages.
 *
 * @author Matt Coley
 */
public final class TypeVisibility {
	private TypeVisibility() {}

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
}
