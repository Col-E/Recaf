package me.coley.recaf.workspace.resource;

/**
 * File info for resource.
 *
 * @author Matt Coley
 */
public class FileInfo extends ItemInfo {
	/**
	 * @param name
	 * 		Internal class name.
	 * @param value
	 * 		File content.
	 */
	public FileInfo(String name, byte[] value) {
		super(name, value);
	}
}
