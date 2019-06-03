package me.coley.recaf.event;

import me.coley.event.Event;

/**
 * Event for requesting a class be reloaded in the UI. There may be a new name for the class,
 * {@link #getNewName()}. If not it will be the same value as {@link #getName()}.
 *
 * @author Matt
 */
public class ClassReloadEvent extends Event {
	private final String name, newName;

	public ClassReloadEvent(String name) {
		this(name, name);
	}

	public ClassReloadEvent(String name, String newName) {
		this.name = name;
		this.newName = newName;
	}

	/**
	 * @return Name of the class updated.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return New name of the class updated.
	 */
	public String getNewName() {
		return newName;
	}
}