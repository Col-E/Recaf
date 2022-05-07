package me.coley.recaf.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;

/**
 * JavaFX node event handler utils.
 *
 * @author Matt Coley
 */
public class NodeEvents {
	/**
	 * @param node
	 * 		Node to add to.
	 * @param handler
	 * 		Handler to add.
	 */
	public static void addKeyPressHandler(Node node, EventHandler<? super KeyEvent> handler) {
		EventHandler<? super KeyEvent> oldHandler = node.getOnKeyPressed();
		node.setOnKeyPressed(e -> {
			if (oldHandler != null)
				oldHandler.handle(e);
			handler.handle(e);
		});
	}
}
