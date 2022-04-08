package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Dummy content source that will provide no data.
 *
 * @author Matt Coley
 */
public class EmptyContentSource extends ContentSource {
	/**
	 * Create empty content source.
	 */
	public EmptyContentSource() {
		super(SourceType.EMPTY);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		// no-op
	}
}
