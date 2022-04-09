package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;

/**
 * Listener for read/write operations of a {@link ContentSource}.
 *
 * @author Matt Coley
 */
public interface ContentSourceListener {
	/**
	 * Called before {@link ContentSource#readInto(Resource)} is invoked.
	 * Any pre-processing steps can be done here.
	 *
	 * @param collection
	 * 		Destination.
	 */
	void onPreRead(ContentCollection collection);

	/**
	 * Called after {@link ContentSource#readInto(Resource)} completes.
	 * Any cleanup steps can be done here.
	 *
	 * @param collection
	 * 		Destination.
	 */
	void onFinishRead(ContentCollection collection);
}
