package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;

/**
 * Models a variable.
 *
 * @param name
 * 		Name of label.
 * @param usage
 * 		Usages of the loabel in the AST.
 */
public record LabelData(@Nonnull String name, @Nonnull AstUsages usage) {
}
