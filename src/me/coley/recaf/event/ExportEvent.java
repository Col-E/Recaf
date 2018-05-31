package me.coley.recaf.event;

import java.io.File;
import java.util.Map;

import me.coley.event.Event;

/**
 * Event for requesting exporting.
 * 
 * @author Matt
 */
public class ExportEvent extends Event {
	private final File file;
	private final Map<String, byte[]> contents;

	public ExportEvent(File file, Map<String, byte[]> contents) {
		this.file = file;
		this.contents = contents;
	}

	/**
	 * @return Location to save file to.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return Map of contents to be written to the {@link #getFile() output}
	 *         file.
	 */
	public Map<String, byte[]> getContents() {
		return contents;
	}
}