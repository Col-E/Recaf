package me.coley.recaf.workspace;

import me.coley.event.Listener;
import me.coley.recaf.event.ResourceUpdateEvent;

import java.util.HashMap;

/**
 * FileMap for input resource files.
 *
 * @author Matt
 */
public class ResourcesMap extends HashMap<String, byte[]> {
	@Listener
	private void onUpdate(ResourceUpdateEvent event) {
		if(event.getResource() != null) {
			put(event.getResourceName(), event.getResource());
		} else {
			remove(event.getResourceName());
		}
	}
}
