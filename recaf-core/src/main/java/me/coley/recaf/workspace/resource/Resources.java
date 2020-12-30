package me.coley.recaf.workspace.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper for multiple resources.
 *
 * @author Matt Coley
 */
public class Resources {
	private final Resource primary;
	private final List<Resource> libraries;

	/**
	 * @param primary
	 * 		Primary resource.
	 */
	public Resources(Resource primary) {
		this(primary, Collections.emptyList());
	}

	/**
	 * @param primary
	 * 		Primary resource.
	 * @param libraries
	 * 		Library resources.
	 */
	public Resources(Resource primary, List<Resource> libraries) {
		this.primary = primary;
		this.libraries = new ArrayList<>(libraries);
	}

	/**
	 * @return Primary resource.
	 */
	public Resource getPrimary() {
		return primary;
	}

	/**
	 * Library resources are used when content is not in {@link #getPrimary() the primary resource}.
	 *
	 * @return Library resources.
	 */
	public List<Resource> getLibraries() {
		return libraries;
	}

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Class wrapper, if present. Otherwise {@code null}.
	 */
	public ClassInfo getClass(String name) {
		// Check primary resource for class
		ClassInfo info = primary.getClasses().get(name);
		if (info == null) {
			// Check libraries for class
			for (Resource resource : libraries) {
				info = resource.getClasses().get(name);
				if (info != null) {
					break;
				}
			}
		}
		return info;
	}

	/**
	 * @param name
	 * 		File path name.
	 *
	 * @return File wrapper, if present. Otherwise {@code null}.
	 */
	public FileInfo getFile(String name) {
		// Check primary resource for file
		FileInfo info = primary.getFiles().get(name);
		if (info == null) {
			// Check libraries for file
			for (Resource resource : libraries) {
				info = resource.getFiles().get(name);
				if (info != null) {
					break;
				}
			}
		}
		return info;
	}
}
