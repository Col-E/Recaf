package me.coley.recaf.workspace;

import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.event.ResourceUpdateEvent;

import java.util.Set;

/**
 * FileMap for input resource files.
 *
 * @author Matt
 */
public class ResourcesMap extends FileMap<String, byte[]> {
	public ResourcesMap(Input input, Set<String> keys) {
		super(input, keys);
	}

	@Listener
	private void onUpdate(ResourceUpdateEvent event) {
		if(event.getResource() == null) {
			remove(event.getResourceName());
		} else {
			cache.remove(event.getResourceName());
		}
	}

	@Override
	byte[] castValue(byte[] file) {
		return file;
	}

	@Override
	byte[] castBytes(byte[] value) {
		return value;
	}

	@Override
	String castKey(Object in) {
		return in.toString();
	}
}
