package me.coley.recaf.event;

import me.coley.event.Event;

/**
 * Event for when a class load is intercepted by instrumentation.
 * 
 * @author Matt
 */
public class ClassLoadInstrumentedEvent extends Event {
	private final String name;

	public ClassLoadInstrumentedEvent(String name) {
		this.name = name;
	}

	/**
	 * @return Name of class loaded.
	 */
	public String getName() {
		return name;
	}
}