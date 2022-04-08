package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
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
				// Check if class can be parsed by ASM
				try {
					if (isParsableClass(content)) {
						// Class can be parsed, record it as a class
						ClassInfo clazz = ClassInfo.read(content);
						// First make sure it has a path name that can be referenced at runtime.
						// We want to intercept junk data, so we can optionally toss it.
						String nameInPackage = StringUtil.shortenPath(clazz.getName());
						if (nameInPackage.isEmpty()) {
							// Alert the user via log call that something is amis
							String loggedName = name;
							if (loggedName.length() > 20)
								loggedName = loggedName.substring(0, 20);
							logger.warn("Adding unreferenceable class '{}' as a file instead", loggedName);
							FileInfo file = new FileInfo(name, content);
							resource.getFiles().initialPut(file);
							return;
						}
						// Class is normal enough.
						getListeners().forEach(l -> l.onClassEntry(clazz));
						resource.getClasses().initialPut(clazz);
					} else {
						// Class cannot be parsed, record it as a file
						int classExtIndex = name.lastIndexOf(".class");
						if (classExtIndex != -1) {
							name = name.substring(0, classExtIndex);
						}
						name = filterInputClassName(name);
						FileInfo clazz = new FileInfo(name + ".class", content);
						getListeners().forEach(l -> l.onInvalidClassEntry(clazz));
						resource.getFiles().initialPut(clazz);
					}
				} catch (Exception ex) {
					logger.warn("Uncaught exception parsing class '{}' from input", name, ex);
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
}