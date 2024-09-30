package software.coley.recaf.workspace.model.resource;

import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;

import java.util.Map;
import java.util.NavigableMap;

/**
 * Builder for {@link WorkspaceFileResource}.
 *
 * @author Matt Coley
 */
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
	 * @param other
	 * 		Builder to copy from.
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

	@Override
	public WorkspaceFileResourceBuilder withJvmClassBundle(JvmClassBundle primaryJvmClassBundle) {
		return (WorkspaceFileResourceBuilder) super.withJvmClassBundle(primaryJvmClassBundle);
	}

	@Override
	public WorkspaceFileResourceBuilder withVersionedJvmClassBundles(NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles) {
		return (WorkspaceFileResourceBuilder) super.withVersionedJvmClassBundles(versionedJvmClassBundles);
	}

	@Override
	public WorkspaceFileResourceBuilder withAndroidClassBundles(Map<String, AndroidClassBundle> androidClassBundles) {
		return (WorkspaceFileResourceBuilder) super.withAndroidClassBundles(androidClassBundles);
	}

	@Override
	public WorkspaceFileResourceBuilder withFileBundle(FileBundle primaryFileBundle) {
		return (WorkspaceFileResourceBuilder) super.withFileBundle(primaryFileBundle);
	}

	@Override
	public WorkspaceFileResourceBuilder withEmbeddedResources(Map<String, WorkspaceFileResource> embeddedResources) {
		return (WorkspaceFileResourceBuilder) super.withEmbeddedResources(embeddedResources);
	}

	@Override
	public WorkspaceFileResourceBuilder withContainingResource(WorkspaceResource containingResource) {
		return (WorkspaceFileResourceBuilder) super.withContainingResource(containingResource);
	}
}
