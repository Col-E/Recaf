package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Origin location information and loading for {@link Resource}s.
 *
 * @author Matt Coley
 */
public abstract class ContentSource {
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
		getListeners().forEach(l -> l.onPreRead(resource));
		onRead(resource);
		getListeners().forEach(l -> l.onFinishRead(resource));
	}

	/**
	 * Reads classes and files from the source and deposits them into the given resource.
	 *
	 * @param resource
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	protected abstract void onRead(Resource resource) throws IOException;

	/**
	 * @return Content source type.
	 */
	public SourceType getType() {
		return type;
	}
}
