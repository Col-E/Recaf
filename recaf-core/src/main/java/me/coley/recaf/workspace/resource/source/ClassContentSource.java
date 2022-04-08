package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Origin location information for classes.
 *
 * @author Matt Coley
 */
public class ClassContentSource extends FileContentSource {
	private static final Logger logger = Logging.get(ClassContentSource.class);

	/**
	 * @param path
	 * 		Path to class file.
	 */
	public ClassContentSource(Path path) {
		super(SourceType.CLASS, path);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		byte[] content;
		try (InputStream stream = Files.newInputStream(getPath())) {
			content = IOUtil.toByteArray(stream);
		} catch (Exception ex) {
			throw new IOException("Failed to load class '" + getPath().getFileName() + "'", ex);
		}
		if (isParsableClass(content)) {
			ClassInfo clazz = ClassInfo.read(content);
			getListeners().forEach(l -> l.onClassEntry(clazz));
			resource.getClasses().initialPut(clazz);
		} else {
			String name = getPath().getFileName().toString();
			FileInfo clazz = new FileInfo(name, content);
			getListeners().forEach(l -> l.onInvalidClassEntry(clazz));
			resource.getFiles().initialPut(clazz);
		}
	}
}
