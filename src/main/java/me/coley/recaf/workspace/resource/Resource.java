package me.coley.recaf.workspace.resource;

import me.coley.recaf.workspace.resource.source.ContentSource;

/**
 * Workspace unit.
 *
 * @author Matt Coley
 */
public class Resource {
	private final ClassMap classes = new ClassMap(this);
	private final FileMap files = new FileMap(this);
	private final ContentSource contentSource;

	/**
	 * Create the resource from the given content source.
	 *
	 * @param contentSource
	 * 		Source of content, containing classes and files.
	 */
	public Resource(ContentSource contentSource) {
		this.contentSource = contentSource;
	}

	/**
	 * @return Collection of the classes contained by the resource.
	 */
	public ClassMap getClasses() {
		return classes;
	}

	/**
	 * @return Collection of the files contained by the resource.
	 */
	public FileMap getFiles() {
		return files;
	}

	/**
	 * The content source of a resource contains information about where the content of the resource was loaded from.
	 * For example, a jar,war,zip,url,etc.
	 * <br>
	 * Internally it will provide access to loading from the content.
	 *
	 * @return Content location information.
	 */
	public ContentSource getContentSource() {
		return contentSource;
	}

	/**
	 * @param classListener
	 * 		Resource listener for class updates.
	 */
	public void setClassListener(ResourceItemListener<ClassInfo> classListener) {
		classes.setListener(classListener);
	}

	/**
	 * @param fileListener
	 * 		Resource listener for file updates.
	 */
	public void setFileListener(ResourceItemListener<FileInfo> fileListener) {
		files.setListener(fileListener);
	}
}
