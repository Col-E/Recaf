package me.coley.recaf.util;

import javafx.scene.Node;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Icon and graphic utilities.
 *
 * @author Matt Coley
 */
public class IconUtil {
	/**
	 * @param resource
	 * 		Resource to represent.
	 *
	 * @return Node to represent the resource.
	 */
	public static Node getIconForResource(Resource resource) {
		// TODO: Different icons for different content sources
		return new IconView("icons/jar.png");
	}
}
