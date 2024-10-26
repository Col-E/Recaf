package software.coley.recaf.analytics.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import jakarta.annotation.Nonnull;

/**
 * Logging filter impl that only allows Recaf logger calls.
 *
 * @author Matt Coley
 */
public class RecafLoggingFilter extends Filter<ILoggingEvent> {
	@Override
	public FilterReply decide(@Nonnull ILoggingEvent event) {
		String loggerName = event.getLoggerName();
		if (loggerName.startsWith("software.coley.") || Logging.loggerKeys().contains(loggerName))
			return FilterReply.ACCEPT;
		return FilterReply.DENY;
	}
}