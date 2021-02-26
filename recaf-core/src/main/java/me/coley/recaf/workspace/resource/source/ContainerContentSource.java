package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.FileInfo;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Origin location information of container files <i>(jar, zip, war, directories)</i>.
 *
 * @param <E>
 * 		Container entry type.
 *
 * @author Matt Coley
 */
public abstract class ContainerContentSource<E> extends FileContentSource {
	protected final Logger logger = Logging.get(getClass());
	private Predicate<E> entryFilter = createDefaultFilter();

	protected ContainerContentSource(SourceType type, Path path) {
		super(type, path);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		logger.info("Reading from file: {}", getPath());
		consumeEach((entry, content) -> {
			String name = getPathName(entry);
			if (isClass(entry, content)) {
				int classExtIndex = name.lastIndexOf(".class");
				String className;
				// Check if class can be parsed by ASM
				if (isParsableClass(content)) {
					// Class can be parsed, record it as a class
					ClassInfo clazz = ClassInfo.read(content);
					getListeners().forEach(l -> l.onClassEntry(clazz));
					resource.getClasses().initialPut(clazz);
				} else {
					// Class cannot be parsed, record it as a file
					className = filterInputClassName(name.substring(0, classExtIndex));
					FileInfo clazz = new FileInfo(className + ".class", content);
					getListeners().forEach(l -> l.onInvalidClassEntry(clazz));
					resource.getFiles().initialPut(clazz);
				}
			} else {
				FileInfo file = new FileInfo(name, content);
				getListeners().forEach(l -> l.onFileEntry(file));
				resource.getFiles().initialPut(file);
			}
		});
		// Summarize what has been found
		logger.info("Read {} classes, {} files", resource.getClasses().size(), resource.getFiles().size());
	}

	@Override
	public void onWrite(Resource resource, Path path) throws IOException {
		// Ensure parent directory exists
		Path parentDir = path.getParent();
		if (parentDir != null && !Files.isDirectory(parentDir)) {
			Files.createDirectories(parentDir);
		}
		// Collect content to put into export directory
		SortedMap<String, byte[]> outContent = new TreeMap<>();
		resource.getFiles().forEach((fileName, fileInfo) ->
				outContent.put(fileName, fileInfo.getValue()));
		resource.getClasses().forEach((className, classInfo) ->
				outContent.put(filterOutputClassName(className), classInfo.getValue()));
		// Log dirty classes
		Set<String> dirtyClasses = resource.getClasses().getDirtyItems();
		Set<String> dirtyFiles = resource.getFiles().getDirtyItems();
		logger.info("Attempting to write {} classes, {} files to: {}",
				resource.getClasses().size(), resource.getClasses().size(), path);
		logger.info("{}/{} classes have been modified, {}/{} files have been modified",
				resource.getClasses().size(), dirtyClasses.size(),
				resource.getClasses().size(), dirtyFiles.size());
		if (logger.isDebugEnabled()) {
			dirtyClasses.forEach(name -> logger.debug("Dirty class: " + name));
			dirtyFiles.forEach(name -> logger.debug("Dirty file: " + name));
		}
		// Write to file
		long startTime = System.currentTimeMillis();
		writeContent(path, outContent);
		logger.info("Write complete, took {}ms", System.currentTimeMillis() - startTime);
	}

	/**
	 * @param entryFilter
	 * 		New entry filter.
	 */
	public void setEntryFilter(Predicate<E> entryFilter) {
		this.entryFilter = Objects.requireNonNull(entryFilter);
	}

	/**
	 * Determines what entries of the container are suitable for loading.
	 *
	 * @return Entry filter.
	 */
	public Predicate<E> getEntryFilter() {
		return entryFilter;
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
	 * Writes a map to a container destination.
	 *
	 * @param output
	 * 		File location of container.
	 * @param content
	 * 		Contents to write to location.
	 *
	 * @throws IOException
	 * 		When the container cannot be written to.
	 */
	protected abstract void writeContent(Path output, SortedMap<String, byte[]> content) throws IOException;

	/**
	 * @return Default container filter.
	 */
	protected abstract Predicate<E> createDefaultFilter();

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

	/**
	 * Get the file extension of the entry.
	 *
	 * @param name
	 * 		Entry name.
	 *
	 * @return File name extension if present. Otherwise {@code null}.
	 */
	protected static String getExtension(String name) {
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < name.length() - 1) {
			return name.substring(dotIndex + 1);
		} else {
			return null;
		}
	}
}