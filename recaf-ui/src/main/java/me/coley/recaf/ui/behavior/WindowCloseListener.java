package me.coley.recaf.ui.behavior;

import javafx.stage.WindowEvent;

/**
 * Used on controls placed inside {@link me.coley.recaf.ui.window.GenericWindow} that need to run actions once
 * the window is closed.
 *
 * @author Matt Coley
 */
public interface WindowCloseListener {
	/**
	 * Invoked on the UI thread.
	 *
	 * @param e
	 * 		Window event.
	 */
	void onClose(WindowEvent e);
}
