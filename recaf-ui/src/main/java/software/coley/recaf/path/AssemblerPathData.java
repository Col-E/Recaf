package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.query.resolution.Resolution;
import software.coley.recaf.ui.control.richtext.Editor;

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
public record AssemblerPathData(@Nonnull Editor editor, @Nonnull Resolution resolution) {
}
