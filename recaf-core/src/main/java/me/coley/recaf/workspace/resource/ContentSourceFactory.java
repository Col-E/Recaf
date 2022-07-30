package me.coley.recaf.workspace.resource;

import me.coley.recaf.workspace.resource.source.ContentSource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Content source factory.
 *
 * @author xDark
 */
public interface ContentSourceFactory {

	/**
	 * @param path
	 * 		Content path.
	 *
	 * @return Content source.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	ContentSource create(Path path) throws IOException;

	/**
	 * @param url
	 * 		URL to create source from.
	 *
	 * @return Content source.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	ContentSource create(URL url) throws IOException;

	/**
	 * @return Default content factory.
	 */
	static ContentSourceFactory defaultFactory() {
		return DefaultContentSourceFactory.INSTANCE;
	}
}
