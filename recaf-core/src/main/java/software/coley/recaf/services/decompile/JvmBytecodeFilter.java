package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Used to allow interception of bytecode passed to {@link JvmDecompiler} instances.
 *
 * @author Matt Coley
 */
public interface JvmBytecodeFilter {
	/**
	 * @param workspace
	 * 		The workspace the class is from.
	 * @param initialClassInfo
	 * 		Initial information about the class, before any filtering <i>(by other filters)</i> has been applied.
	 * 		Contains a reference to the original bytecode.
	 * @param bytecode
	 * 		Input JVM class bytecode. May already be modified from the original bytecode by another filter.
	 *
	 * @return Output JVM class bytecode.
	 */
	@Nonnull
	byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode);
}
