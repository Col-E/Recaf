package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The type of content source.
 *
 * @author Matt Coley
 */
public enum SourceType {
	/**
	 * Multiple files in an Java archive.
	 */
	JAR,
	/**
	 * Multiple files in an archive. For web applications.
	 */
	WAR,
	/**
	 * Java modules archive.
	 */
	JMOD,
	/**
	 * Java packed modules file.
	 */
	MODULES,
	/**
	 * Multiple files in an archive. For Android. Classes packed into single file.
	 */
	APK,
	/**
	 * Multiple files in an archive.
	 */
	ZIP,
	/**
	 * Single file that is not one of the preceding types.
	 */
	SINGLE_FILE,
	/**
	 * Multiple files in a directory.
	 */
	DIRECTORY,
	/**
	 * Multiple files in a maven artifact.
	 */
	MAVEN,
	/**
	 * Content hosted online or locally, should map to a direct file source type.
	 */
	URL,
	/**
	 * Current agent instrumentation.
	 */
	INSTRUMENTATION,
	/**
	 * Dummy resource.
	 */
	EMPTY;

	private static final Logger logger = LoggerFactory.getLogger(SourceType.class);

	/**
	 * @param path
	 * 		Path to file/directory.
	 *
	 * @return Type based on path,
	 * either the file name extension or the path itself for directories.
	 *
	 * @throws IOException
	 * 		When the path cannot be read from.
	 */
	public static SourceType fromPath(Path path) throws IOException {
		if (Files.isDirectory(path))
			return DIRECTORY;
		// Use file header to determine content
		byte[] header = IOUtil.readHeader(path);
		SourceType type = fromHeader(header);
		if (type != null) {
			// Check if ZIP can be made more specific
			if (type == ZIP) {
				String fileName = path.getFileName().toString();
				SourceType extensionType = fromExtension(fileName);
				if (extensionType == JAR || extensionType == APK || extensionType == WAR) {
					return extensionType;
				}
			}
			// Use source type from header
			return type;
		}
		// Unknown file type
		StringBuilder headerText = new StringBuilder();
		StringBuilder headerBytes = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			headerText.append(Character.valueOf((char) header[i]));
			headerBytes.append(StringUtil.toHexString(header[i]));
			if (i < 3) headerBytes.append("-");
		}
		String extension = path.getFileName().toString();
		if (extension.indexOf('.') > 0)
			extension = extension.substring(extension.lastIndexOf('.') + 1);
		logger.warn("Unknown file type (header={}/{}): extension={}", headerText, headerBytes, extension);
		// Go off of path name.
		String fileName = path.getFileName().toString();
		return fromExtension(fileName);
	}

	/**
	 * @param url
	 * 		URL to file.
	 *
	 * @return Type based on file name extension.
	 */
	public static SourceType fromUrl(java.net.URL url) {
		return fromExtension(url.getFile());
	}

	/**
	 * @param path
	 * 		File path name.
	 *
	 * @return Type based on file name extension.
	 */
	public static SourceType fromExtension(String path) {
		path = StringUtil.shortenPath(path.toLowerCase());
		if (path.endsWith(".jar")) {
			return SourceType.JAR;
		} else if (path.endsWith(".zip")) {
			return SourceType.ZIP;
		} else if (path.endsWith(".apk")) {
			return SourceType.APK;
		} else if (path.endsWith(".jmod")) {
			return SourceType.JMOD;
		} else if (path.endsWith(".war")) {
			return SourceType.WAR;
		} else if (path.equals("modules")) {
			// JDK modules file is just called 'modules'
			return SourceType.MODULES;
		}
		// Default case
		return SourceType.SINGLE_FILE;
	}

	/**
	 * @param header
	 * 		First section of bytes of the file. Ideally 16.
	 *
	 * @return Source type matching the given header.
	 */
	public static SourceType fromHeader(byte[] header) {
		// Check against known headers
		if (ByteHeaderUtil.match(header, ByteHeaderUtil.ZIP)) {
			// Handle most archives (including jars)
			return ZIP;
		} else if (ByteHeaderUtil.match(header, ByteHeaderUtil.CLASS)) {
			return SINGLE_FILE;
		} else if (ByteHeaderUtil.match(header, ByteHeaderUtil.JMOD)) {
			return JMOD;
		} else if (ByteHeaderUtil.match(header, ByteHeaderUtil.MODULES)) {
			return MODULES;
		}
		// Unhandled case.
		return null;
	}

	/**
	 * @param path
	 * 		Path to associate source type with.
	 *
	 * @return Content source of the current type with the given path.
	 */
	public ContentSource sourceFromPath(Path path) {
		switch (this) {
			case JAR:
				return new JarContentSource(path);
			case WAR:
				return new WarContentSource(path);
			case JMOD:
				return new JModContainerSource(path);
			case MODULES:
				return new ModulesContainerSource(path);
			case APK:
				return new ApkContentSource(path);
			case ZIP:
				return new ZipContentSource(path);
			case SINGLE_FILE:
				return new SingleFileContentSource(path);
			case DIRECTORY:
				return new DirectoryContentSource(path);
			default:
				throw new IllegalStateException("SourceType " + name() + " cannot be associated with a path!");
		}
	}
}
