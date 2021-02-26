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
		return fromPath(path, read, null);
	}

	/**
	 * @param path
	 * 		Path to some file or directory.
	 * @param read
	 * 		Flag for if the resource should {@link Resource#read() parse} its content immediately.
	 * @param listener
	 * 		Listener to add to the {@link ContentSource}.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		When the resource could not be read from.
	 */
	public static Resource fromPath(Path path, boolean read, ContentSourceListener listener) throws IOException {
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
			else if (pathStr.endsWith(".apk"))
				source = new ApkContentSource(path);
			else
				throw new IOException("Unhandled file type: " + pathStr);
		}
		// Add listener if given
		// TODO: Make it so when this method is called the plugin manager can pass in
		//   a listener singleton that calls plugins when the relevant action occurs.
		if (listener != null) {
			source.addListener(listener);
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
