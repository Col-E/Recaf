package software.coley.recaf.services.workspace.patch.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.util.StringDiff;

import java.util.List;

/**
 * Patches for a single text file.
 *
 * @param path
 * 		Path to the text file to patch.
 * @param textDiffs
 * 		Text patches to apply to the text file.
 *
 * @author Matt Coley
 */
public record TextFilePatch(@Nonnull FilePathNode path, @Nonnull List<StringDiff.Diff> textDiffs) {
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TextFilePatch that = (TextFilePatch) o;

		if (path.localCompare(that.path) != 0)
			return false;
		return textDiffs.equals(that.textDiffs);
	}

	@Override
	public int hashCode() {
		return textDiffs.hashCode();
	}
}
