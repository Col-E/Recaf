package software.coley.recaf.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Wrapper for a resource to assist in handling of mixed classpath and non-classpath items.
 *
 * @author Matt Coley
 */
public class InternalPath {
	private final String path;
	private final boolean internal;

	/**
	 * Create a resource wrapper.
	 *
	 * @param path
	 * 		Path to resource.
	 * @param internal
	 *        {@code true} if the resource is in the classpath, {@code false} if it is external.
	 */
	private InternalPath(String path, boolean internal) {
		this.path = path;
		this.internal = internal;
	}

	/**
	 * Create an internal resource.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return Internal resource wrapper.
	 */
	public static InternalPath internal(String path) {
		return new InternalPath(path, true);
	}

	/**
	 * Create an external resource.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return External resource wrapper.
	 */
	public static InternalPath external(String path) {
		return new InternalPath(path, false);
	}

	/**
	 * @return Resource path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @return {@code true} if the resource is in the classpath, {@code false} if it is external.
	 */
	public boolean isInternal() {
		return internal;
	}

	/**
	 * @return Name of resource file.
	 */
	public String getFileName() {
		String name = getPath();
		int sep = name.lastIndexOf('/');
		if (sep > 0)
			name = name.substring(sep + 1);
		return name;
	}

	/**
	 * Creates a URL path to resource,
	 *
	 * @return URL path to resource.
	 *
	 * @throws IOException
	 * 		When the path cannot be created.
	 */
	public URL getURL() throws IOException {
		if (internal) {
			return InternalPath.class.getClassLoader().getResource(getPath());
		} else {
			return new File(getPath()).toURI().toURL();
		}
	}
}