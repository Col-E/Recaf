package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import software.coley.recaf.services.assembler.AssemblerPipeline;

import java.util.List;

/**
 * Outline of a component that takes in the parse results of an {@link AssemblerPipeline}.
 *
 * @author Matt Coley
 */
public interface AssemblerAstConsumer {
	/**
	 * Called when {@link AssemblerPane} parses some AST.
	 *
	 * @param astElements
	 * 		The parsed AST. Verbosity of contents dependent on the phase.
	 * @param phase
	 * 		AST parse phase.
	 */
	void consumeAst(@Nonnull List<ASTElement> astElements, @Nonnull AstPhase phase);
}
