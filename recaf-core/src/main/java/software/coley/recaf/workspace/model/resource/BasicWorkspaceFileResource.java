package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;

import java.util.Objects;

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
		this.fileInfo = Objects.requireNonNull(builder.getFileInfo(), "Cannot construct file resource without an associated file");
	}

	@Nonnull
	@Override
	public FileInfo getFileInfo() {
		return fileInfo;
	}

	@Override
	public String toString() {
		return super.toString() + " " + fileInfo.getName();
	}
}
