package me.coley.recaf.event;

import java.io.File;

import me.coley.event.Event;

/**
 * Event for requesting exporting.
 * 
 * @author Matt
 */
public class RequestExportEvent extends Event {
	private final File file;

	public RequestExportEvent(File file) {
		this.file = file;
	}

	/**
	 * @return Location to save file to.
	 */
	public File getFile() {
		return file;
	}
}