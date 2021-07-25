package me.coley.recaf.code;

import java.util.Locale;

/**
 * File info for resource.
 *
 * @author Matt Coley
 */
public class FileInfo extends LiteralInfo {
	/**
	 * Default extension value assuming the file name does not have an extension.
	 */
	public static final String UNKNOWN_EXT = "";
	private final String extension;

	/**
	 * @param name
	 * 		Internal class name.
	 * @param value
	 * 		File content.
	 */
	public FileInfo(String name, byte[] value) {
		super(name, value);
		// Extract extension
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			extension = name.substring(dotIndex + 1).toLowerCase(Locale.ENGLISH);
		} else {
			extension = UNKNOWN_EXT;
		}
	}

	/**
	 * @return File extension, lower case.
	 */
	public String getExtension() {
		return extension;
	}
}
