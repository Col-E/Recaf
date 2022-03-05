package me.coley.recaf.scripting.impl;

import javafx.stage.FileChooser;
import me.coley.recaf.RecafUI;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;

/**
 * Utility functions for creating basic file dialogs.
 *
 * @author Wolfie / win32kbase
 */
public class DialogAPI {
	private static final Logger logger = Logging.get(DialogAPI.class);

	/**
	 * @return Single file selection. Will be {@code null} if user cancels.
	 */
	public static File singleOpenFileDialog() {
		FileChooser chooser = new FileChooser();
		return chooser.showOpenDialog(RecafUI.getWindows().getMainWindow());
	}

	/**
	 * @return Single file selection. Will be {@code null} if user cancels.
	 */
	public static File singleSaveFileDialog() {
		FileChooser chooser = new FileChooser();
		return chooser.showSaveDialog(RecafUI.getWindows().getMainWindow());
	}

	/**
	 * @return Multiple file selection. Will be {@code null} if user cancels.
	 */
	public static List<File> multipleOpenFileDialog() {
		FileChooser chooser = new FileChooser();
		return chooser.showOpenMultipleDialog(RecafUI.getWindows().getMainWindow());
	}
}
