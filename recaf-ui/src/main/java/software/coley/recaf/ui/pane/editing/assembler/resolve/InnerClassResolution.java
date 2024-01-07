package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTInner;

/**
 * Resolution of an inner class.
 *
 * @param klass
 * 		The associated class declaration which the inner class is attached to.
 * @param inner
 * 		The inner class.
 *
 * @author Matt Coley
 */
public record InnerClassResolution(@Nonnull ASTClass klass, @Nonnull ASTInner inner) implements AssemblyResolution {
}
