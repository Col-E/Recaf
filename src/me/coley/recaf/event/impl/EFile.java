package me.coley.recaf.event.impl;

import java.io.File;

import me.coley.recaf.event.Event;

/**
 * Generic file event.
 * 
 * @author Matt
 */
public abstract class EFile extends Event {
	private File file;

	public EFile(File file) {
		this.file = file;
	}

	/**
	 * @return File pertaining to event source.
	 */
	public File getFile() {
		return file;
	}
}
