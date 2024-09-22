package software.coley.recaf.services.workspace.patch.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;

/**
 * Wrapper of various patches to apply to a workspace.
 *
 * @param workspace
 * 		Workspace to apply the patches to.
 * @param removals
 * 		Removal patches to remove content by paths.
 * @param jvmAssemblerPatches
 * 		Text patches to apply to JVM classes via the assembler.
 * @param textFilePatches
 * 		Text patches to apply to text files.
 *
 * @author Matt Coley
 */
public record WorkspacePatch(@Nonnull Workspace workspace,
                             @Nonnull List<RemovePath> removals,
                             @Nonnull List<JvmAssemblerPatch> jvmAssemblerPatches,
                             @Nonnull List<TextFilePatch> textFilePatches) {

	// TODO: Add more patch type lists
	//  - New classes / files
	//  - Special patch types for specific transformations like:
	//     - "replace this method with 'return 0'"
	//     - "change this method's modifiers"
	//     - Can be JVM/Android agnostic

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WorkspacePatch that = (WorkspacePatch) o;

		if (!jvmAssemblerPatches.equals(that.jvmAssemblerPatches)) return false;
		return textFilePatches.equals(that.textFilePatches);
	}

	@Override
	public int hashCode() {
		int result = jvmAssemblerPatches.hashCode();
		result = 31 * result + textFilePatches.hashCode();
		return result;
	}
}
