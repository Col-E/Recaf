package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Outline for Android/Dalvik decompile capabilities.
 *
 * @author Matt Coley
 */
public interface AndroidDecompiler extends Decompiler {
	// Placeholder until more fleshed out API is implemented
	DecompileResult decompile(@Nonnull Workspace workspace, @Nonnull AndroidClassInfo classInfo);
}
