package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.io.ByteSource;
import me.coley.recaf.io.ByteSourceConsumer;
import me.coley.recaf.io.ByteSourceElement;
import me.coley.recaf.io.ByteSources;
import me.coley.recaf.util.Streams;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

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
		Stream<ByteSourceElement<E>> stream = stream().filter(x -> {
			String name = getPathName(x.getElement());
			// Skip if name contains zero-length directories
			if (name.contains("//"))
				return false;
			// Skip path traversal attempts
			return !name.contains("../");
		});
		//noinspection TryFinallyCanBeTryWithResources
		try {
			Streams.forEachOn(stream, ByteSources.consume((entry, content) -> {
				// Handle content
				String name = getPathName(entry);
				byte[] bytes = content.readAll();
				if (isClass(entry, bytes)) {
					// Check if class can be parsed by ASM
					try {
						if (isParsableClass(bytes)) {
							// Class can be parsed, record it as a class
							ClassInfo clazz = ClassInfo.read(bytes);
							int index = name.lastIndexOf(".class");
							String nameFromPath = index == -1 ? name : name.substring(0, index);
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
							String className = name;
							int classExtIndex = className.lastIndexOf(".class");
							if (classExtIndex != -1)
								className = className.substring(0, classExtIndex);
							collection.addInvalidClass(className, bytes);
						}
					} catch (Exception ex) {
						logger.warn("Uncaught exception parsing class '{}' from input", name, ex);
					}
				} else if (!name.endsWith("/")) {
					// We can skip fake directory entries of non-classes.
					// Now we just read for file contents.
					if (name.endsWith(".class")) {
						collection.addNonClassClass(name, bytes);
					} else {
						FileInfo file = new FileInfo(name, bytes);
						collection.addFile(file);
					}
				}
			}), ThreadUtil::run);
		} finally {
			stream.close();
		}
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
	protected abstract void consumeEach(ByteSourceConsumer<E> entryHandler) throws IOException;

	/**
	 * @return A stream of byte source elements.
	 *
	 * @throws IOException
	 * 		When the container cannot be read from, or when opening an entry stream fails.
	 */
	protected abstract Stream<ByteSourceElement<E>> stream() throws IOException;

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
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	protected abstract boolean isClass(E entry, ByteSource content) throws IOException;

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
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	protected abstract boolean isClass(E entry, byte[] content) throws IOException;

	/**
	 * @param entry
	 * 		Entry instance.
	 *
	 * @return Name of entry.
	 */
	protected abstract String getPathName(E entry);
}