package me.coley.recaf.workspace.resource;

import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.ShortcutUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.resource.source.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Default content source factory.
 *
 * @author xDark
 */
final class DefaultContentSourceFactory implements ContentSourceFactory {
	private static final Logger logger = LoggerFactory.getLogger(DefaultContentSourceFactory.class);
	static final ContentSourceFactory INSTANCE = new DefaultContentSourceFactory();

	private DefaultContentSourceFactory() {
	}

	@Override
	public ContentSource create(Path path) throws IOException {
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
		return source;
	}

	@Override
	public ContentSource create(URL url) throws IOException {
		return new UrlContentSource(url);
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
		} else if (ByteHeaderUtil.match(data, ByteHeaderUtil.MODULES)) {
			return new ModulesContainerSource(path);
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
		logger.warn("Unhandled file type (header={}/{}): {}", headerText, headerBytes, extension);
		return null;
	}
}
