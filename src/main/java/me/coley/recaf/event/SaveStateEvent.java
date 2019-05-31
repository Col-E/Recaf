package me.coley.recaf.event;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import me.coley.event.Event;

/**
 * Event for when a save-state is created. Contains a set of updated classes.
 * 
 * @author Matt
 */
public class SaveStateEvent extends Event {
	private Set<String> classSet = new HashSet<>();

	public SaveStateEvent(String... classes) {
		Collections.addAll(classSet, classes);
	}

	public SaveStateEvent(Collection<String> classes) {
		classSet.addAll(classes);
	}

	/**
	 * @return Set of modified classes.
	 */
	public Set<String> getClasses() {
		return classSet;
	}
}
