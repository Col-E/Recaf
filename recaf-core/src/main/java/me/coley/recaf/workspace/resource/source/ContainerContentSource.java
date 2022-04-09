package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

/**
 * Origin location information of container files <i>(jar, zip, war, directories)</i>.
 *
 * @param <E>
 * 		Container entry type.
 *
 * @author Matt Coley
 */
public abstract class ContainerContentSource<E> extends FileContentSource {
	private static final Logger logger = Logging.get(ContainerContentSource.class);

	protected ContainerContentSource(SourceType type, Path path) {
		super(type, path);
	}

	@Override
	protected void onRead(ContentCollection collection) throws IOException {
		logger.info("Reading from file: {}", getPath());
		consumeEach((entry, content) -> {
			String name = getPathName(entry);
			// Skip if name contains zero-length directories
			if (name.contains("//"))
				return;
			// Skip path traversal attempts
			if (name.contains("../"))
				return;
			// Handle content
			if (isClass(entry, content)) {
				// Check if class can be parsed by ASM
				try {
					if (isParsableClass(content)) {
						// Class can be parsed, record it as a class
						ClassInfo clazz = ClassInfo.read(content);
						String nameFromPath = name.substring(0, name.indexOf(".class"));
						String nameFromClass = clazz.getName();
						// Check if the name in the container does not match the actual class name.
						if (!nameFromClass.equals(nameFromPath)) {
							// Some obfuscators add impossible to reference classes, with names ending in package
							// separator characters (example: 'com/foo/).
							// These are typically used to confuse editors and hold no useful data.
							collection.addMismatchedNameClass(nameFromPath, clazz);
							return;
						}
						// Class is normal enough.
						collection.addClass(clazz);
					} else {
						// Class cannot be parsed, record it as a file
						int classExtIndex = name.lastIndexOf(".class");
						if (classExtIndex != -1)
							name = name.substring(0, classExtIndex);
						collection.addInvalidClass(name, content);
					}
				} catch (Exception ex) {
					logger.warn("Uncaught exception parsing class '{}' from input", name, ex);
				}
			} else if (!name.endsWith("/")) {
				// We can skip fake directory entries of non-classes.
				// Now we just read for file contents.
				if (name.endsWith(".class")) {
					collection.addNonClassClass(name, content);
				} else {
					FileInfo file = new FileInfo(name, content);
					collection.addFile(file);
				}
			}
		});
	}

	/**
	 * Utility for handling loading entries from a container.
	 *
	 * @param entryHandler
	 * 		Entry handler for children to use for content loading.
	 *
	 * @throws IOException
	 * 		When the container cannot be read from, or when opening an entry stream fails.
	 */
	protected abstract void consumeEach(BiConsumer<E, byte[]> entryHandler) throws IOException;

	/**
	 * Determines if the entry is supposedly a class. This is <b>NOT</b> a guarantee the file is a class that can be
	 * parsed by ASM. That responsibility is checked in {@link #isParsableClass(byte[])}.
	 *
	 * @param entry
	 * 		The entry.
	 * @param content
	 * 		The entry value.
	 *
	 * @return {@code true} if the entry indicates it is a class. {@code false} otherwise.
	 */
	protected abstract boolean isClass(E entry, byte[] content);

	/**
	 * @param entry
	 * 		Entry instance.
	 *
	 * @return Name of entry.
	 */
	protected abstract String getPathName(E entry);
}