package me.coley.recaf.event;

import me.coley.event.Event;
import me.coley.logging.Level;

/**
 * Event for logging calls.
 * 
 * @author Matt
 */
public class LogEvent extends Event {
	private final Level level;
	private final String message;

	public LogEvent(Level level, String message) {
		this.level = level;
		this.message = message;
	}

	/**
	 * @return Log level of this message.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * @return Log message content.
	 */
	public String getMessage() {
		return message;
	}

}