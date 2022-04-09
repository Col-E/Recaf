package me.coley.recaf.workspace.resource.source;

import java.io.IOException;

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
	protected void onRead(ContentCollection collection) throws IOException {
		// no-op
	}
}
