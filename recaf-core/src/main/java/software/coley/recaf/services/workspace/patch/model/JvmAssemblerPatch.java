package software.coley.recaf.services.workspace.patch.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.StringDiff;

import java.util.List;

/**
 * Patches for a single text file.
 *
 * @param path
 * 		Path to the JVM class file to patch.
 * @param assemblerDiffs
 * 		Text patches to apply to the class via the assembler.
 *
 * @author Matt Coley
 */
public record JvmAssemblerPatch(@Nonnull ClassPathNode path,
                                @Nonnull List<StringDiff.Diff> assemblerDiffs) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JvmAssemblerPatch that = (JvmAssemblerPatch) o;

		if (path.localCompare(that.path) != 0)
			return false;
		return assemblerDiffs.equals(that.assemblerDiffs);
	}

	@Override
	public int hashCode() {
		return assemblerDiffs.hashCode();
	}
}
