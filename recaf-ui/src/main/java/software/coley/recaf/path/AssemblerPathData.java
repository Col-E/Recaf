package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.pane.editing.assembler.resolve.AssemblyResolution;

/**
 * Wrapper for data associated with an assembler.
 *
 * @param editor
 * 		Editor for the assembler source input.
 * @param resolution
 * 		Resolved point of interaction within the assembler input.
 *
 * @author Matt Coley
 */
public record AssemblerPathData(@Nonnull Editor editor, @Nonnull AssemblyResolution resolution) {
}
