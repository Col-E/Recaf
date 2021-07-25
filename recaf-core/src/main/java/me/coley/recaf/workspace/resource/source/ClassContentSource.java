package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
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
	public void onWrite(Resource resource, Path path) throws IOException {
		if (resource.getClasses().size() > 1) {
			throw new IllegalStateException("Resource was loaded from class, but now has multiple classes!");
		}
		// Ensure parent directory exists
		Path parentDir = path.getParent();
		if (parentDir != null) {
			Files.createDirectories(parentDir);
		}
		// Write
		logger.info("Attempting to class to: {}", path);
		long startTime = System.currentTimeMillis();
		ClassInfo classInfo = resource.getClasses().values().iterator().next();
		Files.write(path, classInfo.getValue());
		logger.info("Write complete, took {}ms", System.currentTimeMillis() - startTime);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		byte[] content;
		try (InputStream stream = Files.newInputStream(getPath())) {
			content = IOUtil.toByteArray(stream);
		} catch(Exception ex) {
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
