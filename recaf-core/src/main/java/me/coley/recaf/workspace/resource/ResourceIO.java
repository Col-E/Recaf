package me.coley.recaf.workspace.resource;

import me.coley.recaf.util.ShortcutUtil;
import me.coley.recaf.util.Unchecked;
import me.coley.recaf.workspace.resource.source.*;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Common IO utilities for reading {@link Resource} instances from paths and urls.
 *
 * @author Matt Coley
 */
public class ResourceIO {

	/**
	 * Reads resource from a path.
	 *
	 * @param sourceFactory
	 * 		Content factory.
	 * @param resourceFactory
	 * 		Resource factory.
	 * @param path
	 * 		Path to read from.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static Resource fromPath(
			ContentSourceFactory sourceFactory,
			ResourceFactory resourceFactory,
			Path path
	) throws IOException {
		path = ShortcutUtil.follow(path);
		return readResource(resourceFactory, sourceFactory.create(path));
	}

	/**
	 * Reads resource from a url.
	 *
	 * @param sourceFactory
	 * 		Content factory.
	 * @param resourceFactory
	 * 		Resource factory.
	 * @param url
	 * 		URL to read from.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static Resource fromUrl(
			ContentSourceFactory sourceFactory,
			ResourceFactory resourceFactory,
			URL url
	) throws IOException {
		ContentSource source = sourceFactory.create(url);
		return readResource(resourceFactory, source);
	}

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
		ResourceFactory factory = ResourceFactory.defaultFactory();
		if (read) {
			factory = factory.autoRead();
		}
		return fromPath(ContentSourceFactory.defaultFactory(), factory, path);
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
	public static Resource fromUrl(URL url, boolean read) throws IOException {
		ResourceFactory factory = ResourceFactory.defaultFactory();
		if (read) {
			factory = factory.autoRead();
		}
		return fromUrl(ContentSourceFactory.defaultFactory(), factory, url);
	}

	private static Resource readResource(ResourceFactory resourceFactory, ContentSource source) {
		return Unchecked.map(input -> {
			ContentSourceListener patcher = new ClassPatchingListener();
			input.addListener(patcher);
			try {
				return resourceFactory.create(input);
			} finally {
				input.removeListener(patcher);
			}
		}, source);
	}
}
