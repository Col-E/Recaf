package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.apache.commons.io.FileUtils;
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
	private final String urlText;

	/**
	 * @param url
	 * 		Url to the content.
	 */
	public UrlContentSource(String url) {
		super(SourceType.URL);
		this.urlText = url;
		backingType = parseType(urlText);
	}

	@Override
	public void writeTo(Resource resource, Path path) throws IOException {
		logger.debug("Redirecting write for URL source to type: {}", backingType.name());
		switch (backingType) {
			case CLASS:
				new ClassContentSource(null).writeTo(resource, path);
				break;
			case JAR:
				new JarContentSource(null).writeTo(resource, path);
				break;
			case WAR:
				new WarContentSource(null).writeTo(resource, path);
				break;
			case ZIP:
				new ZipContentSource(null).writeTo(resource, path);
				break;
			default:
				throw new IllegalStateException("Unsupported backing type for URL content source");
		}
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		boolean isLocal = urlText.startsWith("file:");
		URL url = new URL(urlText);
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
			logger.info("Downloading remote file \"{}\" to temporary local file: {}", urlText, path);
			FileUtils.copyURLToFile(url, path.toFile(), CONNECTION_TIMEOUT, READ_TIMEOUT);
		}
		// Load from local file
		logger.info("Parsing temporary file with backing content source type: {}", backingType.name());
		switch (backingType) {
			case CLASS:
				new ClassContentSource(path).onRead(resource);
				break;
			case JAR:
				new JarContentSource(path).onRead(resource);
				break;
			case WAR:
				new WarContentSource(path).onRead(resource);
				break;
			case ZIP:
				new ZipContentSource(path).onRead(resource);
				break;
			default:
				throw new IllegalStateException("Unsupported backing type for URL content source");
		}
		// Delete if it was a temporary file
		if (!isLocal) {
			logger.info("Done parsing, removing temp file: {}", path);
			FileUtils.deleteQuietly(path.toFile());
		}
	}

	private static SourceType parseType(String urlText) {
		if (urlText.endsWith(".jar")) {
			return SourceType.JAR;
		} else if (urlText.endsWith(".zip")) {
			return SourceType.ZIP;
		} else if (urlText.endsWith(".war")) {
			return SourceType.WAR;
		} else if (urlText.endsWith(".class")) {
			return SourceType.CLASS;
		}
		// Default case
		return SourceType.JAR;
	}

	@Override
	public String toString() {
		return urlText.substring(urlText.lastIndexOf('/') + 1);
	}
}
