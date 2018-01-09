package me.coley.recaf;

import java.io.IOException;

import me.coley.logging.*;

/**
 * Simple logging to console and file.
 * 
 * @author Matt
 */
public class Logging {
	private Logger<?> lgConsole, lgFile;
	private int indentSize = 3;

	public Logging() {
		try {
			lgConsole = new ConsoleLogger(Level.FINE);
			lgFile = new FileLogger("rclog.txt", Level.FINE);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * Print a finely-detailed informational message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public void fine(String message) {
		log(Level.FINE, message);
	}
	
	/**
	 * Print a finely-detailed informational message with an indent level
	 * pre-pended.
	 * 
	 * @param message
	 *            Message to print.
	 * @param indent
	 *            Level of indentation.
	 */
	public void fine(String message, int indent) {
		String formatted = pad(message, (indent * indentSize), ' ');
		log(Level.FINE, formatted);
	}

	/**
	 * Print an informational message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public void info(String message) {
		log(Level.INFO, message);
	}

	/**
	 * Print an informational message with an indent level pre-pended.
	 * 
	 * @param message
	 *            Message to print.
	 * @param indent
	 *            Level of indentation.
	 */
	public void info(String message, int indent) {
		String formatted = pad(message, (indent * indentSize), ' ');
		log(Level.INFO, formatted);
	}

	/**
	 * Print a warning message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public void warn(String message) {
		log(Level.WARN, message);
	}

	/**
	 * Print an error message.
	 * 
	 * @param message
	 *            Message to print.
	 */
	public void error(String message) {
		log(Level.ERRR, message);
	}

	/**
	 * Print an exception and displays it in the UI.
	 * 
	 * @param exception
	 *            Exception to print.
	 */
	public void error(Exception exception) {
		error(exception, true);
	}

	/**
	 * Print an exception.
	 * 
	 * @param exception
	 *            Exception to print.
	 * @param display
	 *            Show error in UI.
	 */
	public void error(Exception exception, boolean display) {
		StringBuilder message = new StringBuilder(exception.getMessage() + "\n");
		for (StackTraceElement element : exception.getStackTrace()) {
			String formatted = pad(element.toString(), indentSize, ' ');
			message.append(formatted + "\n");
		}
		error(message.toString());
		if (display) {
			Recaf.INSTANCE.ui.openException(exception);
		}
	}

	/**
	 * Print the message of the given level.
	 * 
	 * @param level
	 *            Detail level.
	 * @param message
	 *            Message to print.
	 */
	private void log(Level level, String message) {
		lgConsole.log(level, message);
		lgFile.log(level, message);
	}

	/**
	 * Set the console's logging level.
	 * 
	 * @param level
	 *            Detail level.
	 */
	public void setLevelConsole(Level level) {
		setLevel(lgConsole, level);
	}

	/**
	 * Set the file's logging level.
	 * 
	 * @param level
	 *            Detail level.
	 */
	public void setLevelFile(Level level) {
		setLevel(lgFile, level);
	}

	/**
	 * Set the logger's level to the given one.
	 * 
	 * @param logger
	 * @param level
	 */
	private void setLevel(Logger<?> logger, Level level) {
		logger.setLevel(level);
	}

	/**
	 * Pad the given message with the given amount of characters.
	 * 
	 * @param message
	 * @param padding
	 * @param padChar
	 * @return Message with prepended padding.
	 */
	private static String pad(String message, int padding, char padChar) {
		StringBuilder padded = new StringBuilder(message);
		for (int i = 0; i < padding; i++) {
			padded.insert(0, padChar);
		}
		return padded.toString();
	}
}
