package me.coley.recaf.event;

import me.coley.event.Event;

/**
 * Event for when a resource is updated.
 * 
 * @author Matt
 */
public class ResourceUpdateEvent extends Event {
	private final byte[] resource;
	private final String name;

	public ResourceUpdateEvent(String name, byte[] resource) {
		this.resource = resource;
		this.name = name;
	}
	
	/**
	 * @return Resource name.
	 */
	public String getResourceName() {
		return name;
	}

	/**
	 * @return Resource value.
	 */
	public byte[] getResource() {
		return resource;
	}
}