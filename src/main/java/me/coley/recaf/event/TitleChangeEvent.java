package me.coley.recaf.event;

import me.coley.event.Event;

/**
 * Event for changing the main window's title.
 * 
 * @author Matt
 */
public class TitleChangeEvent extends Event {
	private final String title;

	public TitleChangeEvent(String title) {
		this.title = title;
	}

	/**
	 * @return New window title.
	 */
	public String getTitle() {
		return title;
	}
}