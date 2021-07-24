package me.coley.recaf.util;

import me.coley.recaf.util.logging.LogConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

// TODO (should we remove this?)
/**
 * Logger consumer impl that will allow us to print to the configured {@code 'recaf'} logger.
 *
 * @author Matt Coley
 */
public class LoggerConsumerImpl implements LogConsumer<String> {
	private static final Logger logger = LoggerFactory.getLogger("recaf");

	@Override
	public void accept(String loggerName, Level level, String messageContent) {
		switch (level) {
			case ERROR:
				logger.error(messageContent);
				break;
			case WARN:
				logger.warn(messageContent);
				break;
			case INFO:
				logger.info(messageContent);
				break;
			case DEBUG:
				logger.debug(messageContent);
				break;
			case TRACE:
			default:
				logger.trace(messageContent);
				break;
		}
	}

	@Override
	public void accept(String loggerName, Level level, String messageContent, Throwable throwable) {
		switch (level) {
			case ERROR:
				logger.error(messageContent, throwable);
				break;
			case WARN:
				logger.warn(messageContent, throwable);
				break;
			case INFO:
				logger.info(messageContent, throwable);
				break;
			case DEBUG:
				logger.debug(messageContent, throwable);
				break;
			case TRACE:
			default:
				logger.trace(messageContent, throwable);
				break;
		}
	}
}
