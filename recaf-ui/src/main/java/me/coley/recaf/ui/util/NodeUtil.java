package me.coley.recaf.ui.util;

import javafx.collections.ObservableList;
import javafx.scene.Node;

/**
 * JavaFX {@link Node} utilities.
 *
 * @author Matt Coley
 */
public class NodeUtil {
	/**
	 * Adds the given CSS class to the node. Does not create duplicate entries.
	 *
	 * @param node
	 * 		Node instance.
	 * @param className
	 * 		Class name to add.
	 */
	public static void addStyleClass(Node node, String className) {
		ObservableList<String> classes = node.getStyleClass();
		if (!classes.contains(className))
			classes.add(className);
	}

	/**
	 * Removes the given CSS class from the node.
	 *
	 * @param node
	 * 		Node instance.
	 * @param className
	 * 		Class name to add.
	 *
	 * @return {@code true} when the class was removed from the node.
	 */
	public static boolean removeStyleClass(Node node, String className) {
		boolean flag = false;
		while (node.getStyleClass().remove(className))
			flag = true;
		return flag;
	}
}
