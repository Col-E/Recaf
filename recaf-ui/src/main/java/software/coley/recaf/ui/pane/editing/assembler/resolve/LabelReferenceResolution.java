package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of a label reference within a method.
 *
 * @param parentClass
 * 		Class declaring the method. May be {@code null} when the editor is only displaying the method.
 * @param method
 * 		Method declaring the item referencing the given label.
 * @param labelName
 * 		The referenced label name.
 *
 * @author Matt Coley
 */
public record LabelReferenceResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod method, @Nonnull ASTIdentifier labelName) implements AssemblyResolution {
}
