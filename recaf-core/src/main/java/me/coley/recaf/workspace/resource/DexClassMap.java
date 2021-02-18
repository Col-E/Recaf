package me.coley.recaf.workspace.resource;

import java.util.HashMap;

/**
 * Map of Android dex classes in the resource.
 *
 * @author Matt Coley
 */
public class DexClassMap extends ResourceItemMap<DexClassInfo> {
	/**
	 * @param container
	 * 		Parent resource.
	 */
	public DexClassMap(Resource container) {
		super(container, new HashMap<>());
	}
}
