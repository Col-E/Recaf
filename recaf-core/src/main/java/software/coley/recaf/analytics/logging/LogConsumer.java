package software.coley.recaf.analytics.logging;

import org.slf4j.event.Level;

/**
 * Triple-argument consumer for taking in log messages.
 *
 * @param <T>
 * 		Log content.
 *
 * @author Matt Coley
 */
public interface LogConsumer<T> {
	/**
	 * @param loggerName
	 * 		Name of logger message applies to.
	 * @param level
	 * 		Log level of message.
	 * @param messageContent
	 * 		Content of message.
	 */
	void accept(String loggerName, Level level, T messageContent);

	/**
	 * @param loggerName
	 * 		Name of logger message applies to.
	 * @param level
	 * 		Log level of message.
	 * @param messageContent
	 * 		Content of message.
	 * @param throwable
	 * 		Associated thrown exception.
	 */
	void accept(String loggerName, Level level, T messageContent, Throwable throwable);
}
