package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;

/**
 * Resolution of a declared field.
 *
 * @param parentClass
 * 		Class declaring the field. May be {@code null} when the editor is only displaying the field.
 * @param field
 * 		The field.
 *
 * @author Matt Coley
 */
public record FieldResolution(@Nullable ASTClass parentClass, @Nonnull ASTField field) implements AssemblyResolution {
}
