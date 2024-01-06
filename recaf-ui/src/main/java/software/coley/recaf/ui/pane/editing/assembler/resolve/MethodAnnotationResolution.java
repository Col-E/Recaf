package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.specific.ASTAnnotation;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of an annotation attached to a method declaration.
 *
 * @param parentClass
 * 		Class declaring the method.
 * @param targetMethod
 * 		Method the annotation is attached to.
 * @param annotation
 * 		The annotation.
 *
 * @author Matt Coley
 */
public record MethodAnnotationResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod targetMethod, @Nonnull ASTAnnotation annotation) implements AssemblyResolution {
}
