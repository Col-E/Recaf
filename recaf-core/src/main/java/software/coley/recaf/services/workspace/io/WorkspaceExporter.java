package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

/**
 * Outline for supporting exporting of {@link Workspace} back into files.
 *
 * @author Matt Coley
 * @see WorkspaceExportOptions
 */
public interface WorkspaceExporter {
	/**
	 * The actions of exporting are configured by {@link WorkspaceExportOptions}.
	 *
	 * @param workspace
	 * 		The workspace to export.
	 *
	 * @throws IOException
	 * 		When exporting failed for any IO related reason.
	 */
	void export(@Nonnull Workspace workspace) throws IOException;
}
