package me.coley.recaf.ui.window;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * Main window for Recaf.
 *
 * @author Matt Coley
 */
public class MainWindow extends WindowBase {
	@Override
	protected Scene createScene() {
		BorderPane pane = new BorderPane();
		pane.setCenter(new Label("Hello world"));
		if (pane != null)
			throw new IllegalStateException();
		return new Scene(pane);
	}
}
