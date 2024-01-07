package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTField;

/**
 * Resolution of an annotation attached to a field declaration.
 *
 * @param parentClass
 * 		Class declaring the field.
 * @param targetField
 * 		Field the annotation is attached to.
 * @param annotation
 * 		The annotation.
 *
 * @author Matt Coley
 */
public record FieldAnnotationResolution(@Nullable ASTClass parentClass, @Nonnull ASTField targetField, @Nonnull ASTAnnotation annotation) implements AssemblyResolution {
}
