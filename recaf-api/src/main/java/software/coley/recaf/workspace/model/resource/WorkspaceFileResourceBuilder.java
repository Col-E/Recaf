package software.coley.recaf.workspace.model.resource;

import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

public class WorkspaceFileResourceBuilder extends WorkspaceResourceBuilder {
	private FileInfo fileInfo;

	/**
	 * Empty builder.
	 */
	public WorkspaceFileResourceBuilder() {
		// default
	}

	/**
	 * Builder with required inputs.
	 *
	 * @param classes
	 * 		Primary classes.
	 * @param files
	 * 		Primary files.
	 */

	public WorkspaceFileResourceBuilder(JvmClassBundle classes, FileBundle files) {
		super(classes, files);
	}

	/**
	 * @param other Builder to copy from.
	 */
	public WorkspaceFileResourceBuilder(WorkspaceResourceBuilder other) {
		super(other);
		if (other instanceof WorkspaceFileResourceBuilder otherFileBuilder) {
			withFileInfo(otherFileBuilder.getFileInfo());
		}
	}

	@Override
	public WorkspaceFileResourceBuilder withFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
		return this;
	}

	public FileInfo getFileInfo() {
		return fileInfo;
	}

	@Override
	public WorkspaceFileResource build() {
		return new BasicWorkspaceFileResource(this);
	}
}
