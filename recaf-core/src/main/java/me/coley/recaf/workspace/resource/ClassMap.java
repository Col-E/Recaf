package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;

import java.util.HashMap;

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
		super(container, new HashMap<>());
	}
}
