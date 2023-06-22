package software.coley.recaf.workspace.model.resource;

import software.coley.recaf.info.FileInfo;
import software.coley.recaf.workspace.model.bundle.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class WorkspaceResourceBuilder {
	private JvmClassBundle jvmClassBundle = new BasicJvmClassBundle();
	private NavigableMap<Integer, JvmClassBundle> versionedJvmClassBundles = new TreeMap<>();
	private Map<String, AndroidClassBundle> androidClassBundles = Collections.emptyMap();
	private FileBundle fileBundle = new BasicFileBundle();
	private Map<String, WorkspaceFileResource> embeddedResources = Collections.emptyMap();
	private WorkspaceResource containingResource;

	/**
	 * Empty builder.
	 */
	public WorkspaceResourceBuilder() {
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
	public WorkspaceResourceBuilder(JvmClassBundle classes, FileBundle files) {
		withJvmClassBundle(classes);
		withFileBundle(files);
	}

	protected WorkspaceResourceBuilder(WorkspaceResourceBuilder other) {
		withJvmClassBundle(other.getJvmClassBundle());
		withAndroidClassBundles(other.getAndroidClassBundles());
		withVersionedJvmClassBundles(other.getVersionedJvmClassBundles());
		withFileBundle(other.getFileBundle());
		withEmbeddedResources(other.getEmbeddedResources());
		withContainingResource(other.getContainingResource());
	}

	public WorkspaceResourceBuilder withJvmClassBundle(JvmClassBundle primaryJvmClassBundle) {
		this.jvmClassBundle = primaryJvmClassBundle;
		return this;
	}

	public WorkspaceResourceBuilder withVersionedJvmClassBundles(NavigableMap<Integer, JvmClassBundle> versionedJvmClassBundles) {
		this.versionedJvmClassBundles = versionedJvmClassBundles;
		return this;
	}

	public WorkspaceResourceBuilder withAndroidClassBundles(Map<String, AndroidClassBundle> androidClassBundles) {
		this.androidClassBundles = androidClassBundles;
		return this;
	}

	public WorkspaceResourceBuilder withFileBundle(FileBundle primaryFileBundle) {
		this.fileBundle = primaryFileBundle;
		return this;
	}

	public WorkspaceResourceBuilder withEmbeddedResources(Map<String, WorkspaceFileResource> embeddedResources) {
		this.embeddedResources = embeddedResources;
		return this;
	}

	public WorkspaceResourceBuilder withContainingResource(WorkspaceResource containingResource) {
		this.containingResource = containingResource;
		return this;
	}

	public WorkspaceFileResourceBuilder withFileInfo(FileInfo fileInfo) {
		return new WorkspaceFileResourceBuilder(this)
				.withFileInfo(fileInfo);
	}

	public WorkspaceDirectoryResourceBuilder withDirectoryPath(Path directoryPath) {
		return new WorkspaceDirectoryResourceBuilder(this)
				.withDirectoryPath(directoryPath);
	}

	public JvmClassBundle getJvmClassBundle() {
		return jvmClassBundle;
	}

	public NavigableMap<Integer, JvmClassBundle> getVersionedJvmClassBundles() {
		return versionedJvmClassBundles;
	}

	public Map<String, AndroidClassBundle> getAndroidClassBundles() {
		return androidClassBundles;
	}

	public FileBundle getFileBundle() {
		return fileBundle;
	}

	public Map<String, WorkspaceFileResource> getEmbeddedResources() {
		return embeddedResources;
	}

	public WorkspaceResource getContainingResource() {
		return containingResource;
	}

	/**
	 * @return New resource from builder.
	 */
	public WorkspaceResource build() {
		return new BasicWorkspaceResource(this);
	}
}
