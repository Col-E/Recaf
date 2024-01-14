package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Outline for decompilers targeting {@link JvmClassInfo}.
 *
 * @author Matt Coley
 */
public interface JvmDecompiler extends Decompiler {
	/**
	 * @param filter
	 * 		Filter to add.
	 */
	void addJvmInputFilter(@Nonnull JvmInputFilter filter); // TODO: Make config for common defaults (debug stripping, virtual mapping?)

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
