package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;

import java.io.IOException;

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
	 * Reads classes and files from the source and deposits them into the given resource.
	 *
	 * @param resource
	 * 		Destination.
	 *
	 * @throws IOException
	 * 		When reading from the source encounters some error.
	 */
	public abstract void readInto(Resource resource) throws IOException;

	/**
	 * @return Content source type.
	 */
	public SourceType getType() {
		return type;
	}
}
