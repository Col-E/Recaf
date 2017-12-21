package me.coley.recaf.event.impl;

import java.io.File;
import java.util.Map;

/**
 * Created when: User saves a jar file.
 * 
 * @author Matt
 */
public class EFileSave extends EFile {
	private final Map<String, byte[]> contents;

	public EFileSave(File file, Map<String, byte[]> contents) {
		super(file);
		this.contents = contents;
	}

	/**
	 * @return Map of contents of file being saved. Map key is jar entry name,
	 *         value is entry bytes.
	 */
	public Map<String, byte[]> getContents() {
		return contents;
	}
}
