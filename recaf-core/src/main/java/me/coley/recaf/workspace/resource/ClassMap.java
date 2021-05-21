package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Map of classes in the resource.
 *
 * @author Matt Coley
 */
public class ClassMap extends ResourceItemMap<ClassInfo> {
	/**
	 * @param container
	 * 		Parent resource.
	 */
	public ClassMap(Resource container) {
		this(container, new HashMap<>());
	}

	/**
	 * @param container
	 * 		Parent resource.
	 * @param backing
	 * 		Backing map.
	 */
	public ClassMap(Resource container, Map<String, ClassInfo> backing) {
		super(container, backing);
	}
}
