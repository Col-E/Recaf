package software.coley.recaf.workspace.model.resource;

import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;

import java.nio.file.Path;

/**
 * Builder for {@link WorkspaceDirectoryResource}.
 *
 * @author Matt Coley
 */
public class WorkspaceDirectoryResourceBuilder extends WorkspaceResourceBuilder {
	private Path directoryPath;

	/**
	 * Empty builder.
	 */
	public WorkspaceDirectoryResourceBuilder() {
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
	public WorkspaceDirectoryResourceBuilder(JvmClassBundle classes, FileBundle files) {
		super(classes, files);
	}

	/**
	 * @param other
	 * 		Builder to copy from.
	 */
	public WorkspaceDirectoryResourceBuilder(WorkspaceResourceBuilder other) {
		super(other);
		if (other instanceof WorkspaceDirectoryResourceBuilder otherFileBuilder) {
			withDirectoryPath(otherFileBuilder.getDirectoryPath());
		}
	}

	@Override
	public WorkspaceDirectoryResourceBuilder withDirectoryPath(Path directoryPath) {
		this.directoryPath = directoryPath;
		return this;
	}

	public Path getDirectoryPath() {
		return directoryPath;
	}

	@Override
	public WorkspaceDirectoryResource build() {
		return new BasicWorkspaceDirectoryResource(this);
	}
}
