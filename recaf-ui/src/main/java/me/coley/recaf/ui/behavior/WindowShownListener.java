package me.coley.recaf.ui.behavior;

import javafx.stage.WindowEvent;

/**
 * Used on controls placed inside {@link me.coley.recaf.ui.window.GenericWindow} that need to run actions once
 * the window is shown.
 *
 * @author Matt Coley
 */
public interface WindowShownListener {
	/**
	 * Invoked on the UI thread.
	 *
	 * @param e
	 * 		Window event.
	 */
	void onShown(WindowEvent e);
}
