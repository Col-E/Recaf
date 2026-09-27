package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.error.Outcome;

import java.util.List;

/**
 * Observes each parse stage invoked by {@link AssemblerPipeline#fullParse(List, ParseObserver)}.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface ParseObserver {
	/**
	 * Called after a parse stage completes. Later stages rely on prior stages, otherwise they will not be invoked.
	 *
	 * @param stage
	 * 		Parse stage that completed.
	 * @param outcome
	 * 		Stage result, including its diagnostics.
	 */
	void onStage(@Nonnull AssemblyStage stage, @Nonnull Outcome<List<ASTElement>> outcome);
}
