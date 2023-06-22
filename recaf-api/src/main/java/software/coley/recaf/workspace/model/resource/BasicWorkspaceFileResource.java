package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;

/**
 * Basic implementation of a workspace resource sourced from a file.
 *
 * @author Matt Coley
 */
public class BasicWorkspaceFileResource extends BasicWorkspaceResource implements WorkspaceFileResource {
	private final FileInfo fileInfo;

	/**
	 * @param builder
	 * 		Builder to pull info from.
	 */
	public BasicWorkspaceFileResource(WorkspaceFileResourceBuilder builder) {
		super(builder);
		this.fileInfo = builder.getFileInfo();
	}

	@Nonnull
	@Override
	public FileInfo getFileInfo() {
		return fileInfo;
	}
}
