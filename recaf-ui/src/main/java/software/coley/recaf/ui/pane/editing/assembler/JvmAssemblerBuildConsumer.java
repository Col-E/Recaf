package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compiler.ClassRepresentation;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.assembler.AssemblerPipeline;

/**
 * Outline of a component that takes in the build results of an {@link AssemblerPipeline} for JVM classes.
 *
 * @author Matt Coley
 */
public interface JvmAssemblerBuildConsumer extends AssemblerBuildConsumer {
	@Override
	default void consumeClass(@Nonnull ClassRepresentation classRepresentation, @Nonnull ClassInfo classInfo) {
		if (classRepresentation instanceof JavaClassRepresentation jcr && classInfo.isJvmClass())
			onClassAssembled(jcr, classInfo.asJvmClass());
	}

	/**
	 * Called when {@link AssemblerPane} builds a JVM class.
	 *
	 * @param classRepresentation
	 * 		Assembler JVM output representation model.
	 * @param classInfo
	 * 		Recaf JVM class model.
	 */
	void onClassAssembled(@Nonnull JavaClassRepresentation classRepresentation, @Nonnull JvmClassInfo classInfo);
}
