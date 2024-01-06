package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of a declared method.
 *
 * @param parentClass
 * 		Class declaring the method. May be {@code null} when the editor is only displaying the method.
 * @param method
 * 		The method.
 *
 * @author Matt Coley
 */
public record MethodResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod method) implements AssemblyResolution {
}
