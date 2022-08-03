package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.ByteHeaderUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Origin location information of a single file/class.
 *
 * @author Matt Coley
 */
public class SingleFileContentSource extends FileContentSource {
	private static final Logger logger = Logging.get(SingleFileContentSource.class);

	/**
	 * @param path
	 * 		Some path to a file.
	 */
	public SingleFileContentSource(Path path) {
		super(SourceType.SINGLE_FILE, path);
	}

	@Override
	protected void onRead(ContentCollection collection) throws IOException {
		byte[] content;
		try (InputStream stream = Files.newInputStream(getPath())) {
			content = IOUtil.toByteArray(stream);
		} catch (Exception ex) {
			throw new IOException("Failed to load file '" + getPath().getFileName() + "'", ex);
		}
		if (ByteHeaderUtil.match(content, ByteHeaderUtil.CLASS)) {
			try {
				if (isParsableClass(content)) {
					ClassInfo clazz = ClassInfo.read(content);
					collection.addClass(clazz);
				} else {
					String name = getPath().getFileName().toString();
					collection.addInvalidClass(name, content);
				}
			} catch (Exception ex) {
				logger.warn("Uncaught exception parsing class '{}' from input", getPath(), ex);
			}
		} else {
			collection.addFile(new FileInfo(getPath().getFileName().toString(), content));
		}
	}
}
