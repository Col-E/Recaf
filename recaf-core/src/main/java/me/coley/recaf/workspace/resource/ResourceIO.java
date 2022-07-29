package me.coley.recaf.workspace.resource;

import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.ShortcutUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.source.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common IO utilities for reading {@link Resource} instances from paths and urls.
 *
 * @author Matt Coley
 */
public class ResourceIO {
	private static final Logger logger = Logging.get(ResourceIO.class);

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
		path = ShortcutUtil.follow(path);
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
			else if (pathStr.endsWith(".jmod"))
				source = new JModContainerSource(path);
			else
				source = fromHeader(path);
		}
		// Add listener if given as a parameter
		if (listener != null) {
			source.addListener(listener);
		}
		// Add invalid class patcher, read data, remove patcher
		ContentSourceListener patcher = new ClassPatchingListener();
		source.addListener(patcher);
		Resource resource = from(source, read);
		source.removeListener(patcher);
		return resource;
	}

	/**
	 * @param path
	 * 		Path to some file.
	 *
	 * @return Read resource.
	 *
	 * @throws IOException
	 * 		When the resource could not be read from.
	 */
	private static ContentSource fromHeader(Path path) throws IOException {
		// Read first few bytes
		byte[] data = new byte[16];
		try (InputStream fis = Files.newInputStream(path)) {
			fis.read(data);
		} catch (IOException ex) {
			logger.error("Failed to read from file: {} - {}", path, ex);
			throw ex;
		}
		// Check against known headers
		if (ByteHeaderUtil.match(data, ByteHeaderUtil.ZIP)) {
			// Handle most archives (including jars)
			return new ZipContentSource(path);
		} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.JMOD)) {
			return new JModContainerSource(path);
		} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.CLASS)) {
			return new ClassContentSource(path);
		}
		// Well, we tried.
		StringBuilder headerText = new StringBuilder();
		StringBuilder headerBytes = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			headerText.append(Character.valueOf((char) data[i]));
			headerBytes.append(StringUtil.toHexString(data[i]));
			if (i < 3) headerBytes.append("-");
		}
		String extension = path.getFileName().toString();
		if (extension.indexOf('.') > 0)
			extension = extension.substring(extension.lastIndexOf('.') + 1);
		throw new IOException("Unhandled file type (header=" + headerText + "/" + headerBytes + "): " + extension);
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
