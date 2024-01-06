package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.specific.ASTAnnotation;

/**
 * Resolution of an annotation declared independently <i>(Not attached to another AST item)</i>
 *
 * @param annotation
 * 		The annotation.
 *
 * @author Matt Coley
 */
public record IndependentAnnotationResolution(@Nonnull ASTAnnotation annotation) implements AssemblyResolution {
}
