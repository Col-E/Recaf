package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of a type reference.
 *
 * @param parentClass
 * 		Class declaring the method. May be {@code null} when the editor is only displaying the method.
 * @param method
 * 		Method declaring the instruction/attribute with a type reference.
 * @param typeName
 * 		The type reference's name.
 *
 * @author Matt Coley
 */
public record TypeReferenceResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod method, @Nonnull ASTIdentifier typeName) implements AssemblyResolution {
}
