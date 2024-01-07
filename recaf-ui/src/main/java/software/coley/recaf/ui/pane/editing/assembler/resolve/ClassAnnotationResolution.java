package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;

/**
 * Resolution of an annotation attached to a class declaration.
 *
 * @param targetClass
 * 		Class the annotation is attached to.
 * @param annotation
 * 		The annotation.
 *
 * @author Matt Coley
 */
public record ClassAnnotationResolution(@Nonnull ASTClass targetClass, @Nonnull ASTAnnotation annotation) implements AssemblyResolution {
}
