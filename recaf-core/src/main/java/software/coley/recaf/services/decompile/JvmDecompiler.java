package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Outline for decompilers targeting {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public interface JvmDecompiler extends Decompiler {
	/**
	 * Adds a filter which operates on the bytecode of classes before passing it along to the decompiler.
	 *
	 * @param filter
	 * 		Filter to add.
	 *
	 * @return {@code true} on successful addition.
	 * {@code false} if the filter has already been added.
	 */
	boolean addJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter); // TODO: Make config for common defaults (debug stripping, virtual mapping?)

	/**
	 * Removes a filter which operates on the bytecode of classes before passing it along to the decompiler.
	 *
	 * @param filter
	 * 		Filter to remove.
	 *
	 * @return {@code true} on successful removal.
	 * {@code false} if the filter was not already registered.
	 */
	boolean removeJvmBytecodeFilter(@Nonnull JvmBytecodeFilter filter);

	/**
	 * @param workspace
	 * 		Workspace to pull data from.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Decompilation result.
	 */
	@Nonnull
	DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull JvmClassInfo classInfo);
}
