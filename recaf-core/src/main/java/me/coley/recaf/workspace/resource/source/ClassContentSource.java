package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(ClassContentSource.class);

	/**
	 * @param path
	 * 		Path to class file.
	 */
	public ClassContentSource(Path path) {
		super(SourceType.CLASS, path);
	}

	@Override
	public void writeTo(Resource resource, Path path) throws IOException {
		if (resource.getClasses().size() > 1) {
			throw new IllegalStateException("Resource was loaded from class, but now has multiple classes!");
		}
		// Ensure parent directory exists
		Path parentDir = path.getParent();
		if (parentDir != null && !Files.isDirectory(parentDir)) {
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
		try (InputStream stream = Files.newInputStream(getPath())) {
			byte[] content = IOUtils.toByteArray(stream);
			String className = new ClassReader(content).getClassName();
			resource.getClasses().initialPut(new ClassInfo(className, content));
		} catch(Exception ex) {
			throw new IOException("Failed to load class '" + getPath().getFileName() + "'", ex);
		}
	}
}
