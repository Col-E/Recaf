package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.FileInfo;

import java.util.HashMap;

/**
 * Map of files in the resource.
 *
 * @author Matt Coley
 */
public class FileMap extends ResourceItemMap<FileInfo> {
	/**
	 * @param container
	 * 		Parent resource.
	 */
	protected FileMap(Resource container) {
		super(container, new HashMap<>());
	}
}
