package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.resource.Resource;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Origin location information and loading for {@link Resource}s.
 *
 * @author Matt Coley
 */
public abstract class ContentSource {
	private static final Logger logger = Logging.get(ContentSource.class);
	private final Set<ContentSourceListener> listeners = new HashSet<>();
	private final SourceType type;

	protected ContentSource(SourceType type) {
		this.type = type;
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeListener(ContentSourceListener listener) {
		listeners.remove(listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addListener(ContentSourceListener listener) {
		listeners.add(listener);
	}

	/**
	 * @return Listeners.
	 */
	public Set<ContentSourceListener> getListeners() {
		return listeners;
	}

	/**
	 * Populates the given resource with classes and files from the current content source.
	 *
	 * @param resource
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	public void readInto(Resource resource) throws IOException {
		ContentCollection collection = new ContentCollection(resource);
		getListeners().forEach(l -> l.onPreRead(collection));
		onRead(collection);
		getListeners().forEach(l -> l.onFinishRead(collection));
		// Log results / summarize what has been found
		int actionable = collection.getPendingCount();
		if (actionable > 0) {
			logger.info("Read {} classes, {} files, {} actionable items",
					collection.getClassCount() + collection.getDexClassCount(),
					collection.getFileCount(),
					collection.getPendingCount());
		} else {
			logger.info("Read {} classes, {} files",
					collection.getClassCount() + collection.getDexClassCount(),
					collection.getFileCount());
		}
		// Populate
		collection.getFiles().forEach((path, info) -> {
			resource.getFiles().initialPut(info);
		});
		collection.getClasses().forEach((path, info) -> {
			resource.getClasses().initialPut(info);
		});
		collection.getDexClasses().getBackingMap().forEach((path, map) -> {
			resource.getDexClasses().putDexMap(path, map);
		});
	}

	/**
	 * Reads classes and files from the source and deposits them into the given resource.
	 *
	 * @param collection
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	protected abstract void onRead(ContentCollection collection) throws IOException;

	/**
	 * @return Content source type.
	 */
	public SourceType getType() {
		return type;
	}
}
