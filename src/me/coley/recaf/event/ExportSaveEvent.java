package me.coley.recaf.event;

import java.io.File;
import java.util.Map;

/**
 * Event for requesting exporting.
 * 
 * @author Matt
 */
public class ExportSaveEvent extends ExportRequestEvent {
	private final Map<String, byte[]> contents;

	public ExportSaveEvent(File file, Map<String, byte[]> contents) {
		super(file);
		this.contents = contents;
	}

	/**
	 * @return Map of contents to be written to the {@link #getFile() output}
	 *         file.
	 */
	public Map<String, byte[]> getContents() {
		return contents;
	}
}