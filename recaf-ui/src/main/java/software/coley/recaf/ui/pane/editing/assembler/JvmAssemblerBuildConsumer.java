package software.coley.recaf.ui.pane.editing.assembler;

import jakarta.annotation.Nonnull;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.compiler.ClassRepresentation;
import me.darknet.assembler.compiler.ClassResult;
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
	default void consumeClass(@Nonnull ClassResult result, @Nonnull ClassInfo classInfo) {
		if (result instanceof JavaCompileResult jcr && classInfo.isJvmClass())
			onClassAssembled(jcr, classInfo.asJvmClass());
	}

	/**
	 * Called when {@link AssemblerPane} builds a JVM class.
	 *
	 * @param result
	 * 		Assembler JVM output model.
	 * @param classInfo
	 * 		Recaf JVM class model.
	 */
	void onClassAssembled(@Nonnull JavaCompileResult result, @Nonnull JvmClassInfo classInfo);
}
