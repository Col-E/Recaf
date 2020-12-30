package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Origin location information and loading for {@link Resource}s.
 *
 * @author Matt Coley
 */
public abstract class ContentSource {
	private final SourceType type;

	protected ContentSource(SourceType type) {
		this.type = type;
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
		onPreRead(resource);
		onRead(resource);
		onFinishRead(resource);
	}

	/**
	 * Writes the content of the given resource to the given path.
	 * Each source implementation may handle content differently based on the expectation of how it was loaded.
	 *
	 * @param resource
	 * 		Resource with content to write.
	 * @param path
	 * 		Path to write to.
	 *
	 * @throws IOException
	 * 		When writing to the given path fails.
	 */
	public abstract void writeTo(Resource resource, Path path) throws IOException;

	/**
	 * Called before {@link #onRead(Resource)} is invoked. Any pre-processing steps can be done here.
	 *
	 * @param resource
	 * 		Destination.
	 */
	protected void onPreRead(Resource resource) {
		// no-op
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
	 * Called after {@link #onRead(Resource)} completes. Any cleanup steps can be done here.
	 *
	 * @param resource
	 * 		Destination.
	 */
	protected void onFinishRead(Resource resource) {
		// no-op
	}

	/**
	 * @return Content source type.
	 */
	public SourceType getType() {
		return type;
	}
}
