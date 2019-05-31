package me.coley.recaf.event;

import me.coley.event.Event;

/**
 * Event for when a class has reverted to a previous save-state.
 * 
 * @author Matt
 */
public class HistoryRevertEvent extends Event {
	private final String name;

	public HistoryRevertEvent(String name) {
		this.name = name;
	}

	/**
	 * @return Name of class reverted.
	 */
	public String getName() {
		return name;
	}
}