package software.coley.recaf.services.decompile.filter;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Used to allow interception of decompiler output before being returned to users.
 *
 * @author Matt Coley
 */
public interface OutputTextFilter {
	/**
	 * @param workspace
	 * 		The workspace the class is from.
	 * @param classInfo
	 * 		Information about the class the decompiled code models.
	 * @param code
	 * 		Decompiled code.
	 *
	 * @return Filtered decompiled code.
	 */
	@Nonnull
	String filter(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo, @Nonnull String code);
}
