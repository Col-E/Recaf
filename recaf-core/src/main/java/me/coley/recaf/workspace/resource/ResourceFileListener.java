package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.FileInfo;

/**
 * Listener for receiving file updates from a {@link Resource}.
 *
 * @author Matt Coley
 */
public interface ResourceFileListener {
	/**
	 * Called when a new file is added.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param newValue
	 * 		File added to the resource.
	 */
	void onNewFile(Resource resource, FileInfo newValue);


	/**
	 * Called when an old file is removed.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		File removed from the resource.
	 */
	void onRemoveFile(Resource resource, FileInfo oldValue);

	/**
	 * Called when the old file is replaced by the new one.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Prior file value.
	 * @param newValue
	 * 		New file value.
	 */
	void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue);
}
