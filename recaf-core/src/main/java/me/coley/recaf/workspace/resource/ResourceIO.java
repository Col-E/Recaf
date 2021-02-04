package me.coley.recaf.workspace.resource;

import me.coley.recaf.workspace.resource.source.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common IO utilities for reading {@link Resource} instances from paths and urls.
 *
 * @author Matt Coley
 */
public class ResourceIO {
	/**
	 * @param path
	 * 		Path to some file or directory.
	 * @param read
	 * 		Flag for if the resource should {@link Resource#read() parse} its content immediately.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		When the resource could not be read from.
	 */
	public static Resource fromPath(Path path, boolean read) throws IOException {
		String pathStr = path.toString().toLowerCase();
		ContentSource source;
		if (Files.isDirectory(path)) {
			source = new DirectoryContentSource(path);
		} else {
			if (pathStr.endsWith(".jar"))
				source = new JarContentSource(path);
			else if (pathStr.endsWith(".war"))
				source = new WarContentSource(path);
			else if (pathStr.endsWith(".zip"))
				source = new ZipContentSource(path);
			else if (pathStr.endsWith(".class"))
				source = new ClassContentSource(path);
			else
				throw new IOException("Unhandled file type: " + pathStr);
		}
		return from(source, read);
	}

	/**
	 * @param url
	 * 		URL to some file.
	 * @param read
	 * 		Flag for if the resource should {@link Resource#read() parse} its content immediately.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		When the resource could not be read from.
	 */
	public static Resource fromUrl(String url, boolean read) throws IOException {
		return from(new UrlContentSource(url), read);
	}

	private static Resource from(ContentSource source, boolean read) throws IOException {
		Resource resource = new Resource(source);
		if (read)
			resource.read();
		return resource;
	}
}
