package me.coley.recaf.workspace.resource;

import me.coley.recaf.workspace.resource.source.ContentSource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Workspace unit.
 *
 * @author Matt Coley
 */
public class Resource {
	private final ClassMap classes = new ClassMap(this);
	private final FileMap files = new FileMap(this);
	private final MultiDexClassMap dexes = new MultiDexClassMap();
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
	 * Clears any existing data and populates the resource from the {@link #getContentSource() content source}.
	 *
	 * @throws IOException
	 * 		When the {@link #getContentSource() content source} cannot be read from.
	 */
	public void read() throws IOException {
		// Reset
		classes.clear();
		files.clear();
		// Read
		contentSource.readInto(this);
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
	 * @return Collection of the dexes contained by the resource.
	 */
	public MultiDexClassMap getDexClasses() {
		return dexes;
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
	 * Remove all listeners from the resource.
	 */
	public void clearListeners() {
		contentSource.getListeners().clear();
		classes.getListeners().clear();
		files.getListeners().clear();
		dexes.clearListeners();
	}

	/**
	 * @param classListener
	 * 		Resource listener for class updates.
	 */
	public void addClassListener(ResourceClassListener classListener) {
		classes.addListener(CommonItemListener.wrapClass(classListener));
	}

	/**
	 * @param dexClassListener
	 * 		Resource listener for dex class updates.
	 */
	public void addDexListener(ResourceDexClassListener dexClassListener) {
		dexes.addListener(dexClassListener);
	}

	/**
	 * @param fileListener
	 * 		Resource listener for file updates.
	 */
	public void addFileListener(ResourceFileListener fileListener) {
		files.addListener(CommonItemListener.wrapFile(fileListener));
	}
}