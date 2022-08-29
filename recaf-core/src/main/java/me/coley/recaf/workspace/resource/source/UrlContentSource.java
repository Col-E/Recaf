package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Origin location information for content from urls.
 *
 * @author Matt Coley
 */
public class UrlContentSource extends ContentSource {
	private static final Logger logger = Logging.get(UrlContentSource.class);
	private static final int CONNECTION_TIMEOUT = 2000;
	private static final int READ_TIMEOUT = 3000;
	private final SourceType backingType;
	private final URL url;

	/**
	 * @param url
	 * 		Url to the content.
	 */
	public UrlContentSource(URL url) {
		super(SourceType.URL);
		this.url = url;
		backingType = SourceType.fromUrl(url);
	}

	@Override
	protected void onRead(ContentCollection collection) throws IOException {
		boolean isLocal = "file".equals(url.getProtocol());
		Path path;
		if (isLocal) {
			// Fetch path from URI
			try {
				path = Paths.get(url.toURI());
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		} else {
			// Download to temp file
			String extension = backingType.name().toLowerCase();
			path = Paths.get(File.createTempFile("recaf", "temp." + extension).getAbsolutePath());
			logger.info("Downloading remote file \"{}\" to temporary local file: {}", url, path);
			IOUtil.copy(url, path, CONNECTION_TIMEOUT, READ_TIMEOUT);
		}
		// Load from local file
		logger.info("Parsing temporary file with backing content source type: {}", backingType.name());
		backingType.sourceFromPath(path).onRead(collection);
		// Delete if it was a temporary file
		if (!isLocal) {
			logger.info("Done parsing, removing temp file: {}", path);
			IOUtil.deleteQuietly(path);
		}
	}

	/**
	 * @return URL text / original value.
	 */
	public String getUrl() {
		return url.toString();
	}

	@Override
	public String toString() {
		return url.getFile();
	}
}
