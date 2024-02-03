package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.assembler.AssemblerPipeline;

/**
 * Outline of a component that takes in the build results of an {@link AssemblerPipeline}.
 *
 * @author Matt Coley
 * @see JvmAssemblerBuildConsumer For JVM build results.
 */
public interface AssemblerBuildConsumer {
	/**
	 * Called when {@link AssemblerPane} builds a class.
	 *
	 * @param result
	 * 		Assembler output model.
	 * @param classInfo
	 * 		Recaf class model.
	 */
	void consumeClass(@Nonnull ClassResult result, @Nonnull ClassInfo classInfo);
}
