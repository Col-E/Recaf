package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;

/**
 * Resolution of a class name a class extends.
 *
 * @param klass
 * 		The class declaration.
 * @param superName
 * 		The class name being extended.
 *
 * @author Matt Coley
 */
public record ClassExtends(@Nonnull ASTClass klass, @Nonnull ASTIdentifier superName) implements AssemblyResolution {
}
