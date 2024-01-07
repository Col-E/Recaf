package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.primitive.ASTLabel;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of a label declaration within a method.
 *
 * @param parentClass
 * 		Class declaring the method. May be {@code null} when the editor is only displaying the method.
 * @param method
 * 		Method declaring the label.
 * @param label
 * 		The label.
 *
 * @author Matt Coley
 */
public record LabelDeclarationResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod method, @Nonnull ASTLabel label) implements AssemblyResolution {
}
