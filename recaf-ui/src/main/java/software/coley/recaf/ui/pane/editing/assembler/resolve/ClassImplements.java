package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.primitive.ASTIdentifier;
import me.darknet.assembler.ast.specific.ASTClass;

/**
 * Resolution of an interface name a class implements.
 *
 * @param klass
 * 		The class declaration.
 * @param implemented
 * 		The implemented interface name.
 *
 * @author Matt Coley
 */
public record ClassImplements(@Nonnull ASTClass klass, @Nonnull ASTIdentifier implemented) implements AssemblyResolution {
}
