package software.coley.recaf.workspace.io;

import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

/**
 * Outline for supporting exporting of {@link Workspace} back into files.
 *
 * @author Matt Coley
 * @see WorkspaceExportOptions
 * @see WorkspaceManager#createExporter(WorkspaceExportOptions)
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
	void export(Workspace workspace) throws IOException;
}
