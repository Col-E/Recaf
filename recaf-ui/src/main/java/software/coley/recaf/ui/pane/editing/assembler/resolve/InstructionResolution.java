package software.coley.recaf.ui.pane.editing.assembler.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.primitive.ASTInstruction;
import me.darknet.assembler.ast.specific.ASTClass;
import me.darknet.assembler.ast.specific.ASTMethod;

/**
 * Resolution of an instruction within a method.
 *
 * @param parentClass
 * 		Class declaring the method. May be {@code null} when the editor is only displaying the method.
 * @param method
 * 		Method declaring the instruction.
 * @param instruction
 * 		The instruction.
 *
 * @author Matt Coley
 */
public record InstructionResolution(@Nullable ASTClass parentClass, @Nonnull ASTMethod method, @Nonnull ASTInstruction instruction) implements AssemblyResolution {
}
