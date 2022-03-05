package me.coley.recaf.scripting.impl;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceIO;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility functions for making {@link Resource}s.
 *
 * @author Wolfie / win32kbase
 */
public class ResourceAPI {
	private static final Logger logger = Logging.get(ResourceAPI.class);

	/**
	 * @param path
	 * 		Path to load resource from.
	 * @param read
	 *        {@code true} to read the contents of the resource.
	 *
	 * @return Resource from the path.
	 */
	public static Resource createResource(Path path, boolean read) {
		try {
			return ResourceIO.fromPath(path, read);
		} catch (IOException e) {
			logger.error("Failed to create resource {}: {}", path.getFileName(), e.getLocalizedMessage());
			return null;
		}
	}

	/**
	 * @param path
	 * 		Path to load resource from. Automatically reads content.
	 *
	 * @return Resource from the path.
	 */
	public static Resource createResource(String path) {
		return createResource(Paths.get(path), true);
	}

	/**
	 * @param file
	 * 		Path to load resource from. Automatically reads content.
	 * @param read
	 *        {@code true} to read the contents of the resource.
	 *
	 * @return Resource from the path.
	 */
	public static Resource createResource(File file, boolean read) {
		return createResource(file.toPath(), read);
	}

	/**
	 * @param file
	 * 		Path to load resource from. Automatically reads content.
	 *
	 * @return Resource from the path.
	 */
	public static Resource createResource(File file) {
		return createResource(file.toPath(), true);
	}
}
