package software.coley.recaf.analytics.logging;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
	void accept(@Nonnull String loggerName, @Nonnull Level level, @Nullable T messageContent);

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
	void accept(@Nonnull String loggerName, @Nonnull Level level, @Nullable T messageContent, @Nullable Throwable throwable);
}
