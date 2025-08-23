package software.coley.recaf.analytics.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Logging filter impl that only allows Recaf logger calls.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Logging not relevant for test coverage")
public class RecafLoggingFilter extends Filter<ILoggingEvent> {
	/** Shared default level - used by auto-created instances of this filter. */
	public static Level defaultLevel = Level.TRACE;
	/** Instance supplier of the logging level for this filter. */
	private final Supplier<Level> instanceLevel;

	/**
	 * No-args constructor for auto-created instances.
	 * Will delegate the level to {@link #defaultLevel}.
	 */
	public RecafLoggingFilter() {
		instanceLevel = () -> defaultLevel;
	}

	/**
	 * Constructor for intentionally made use cases which
	 * want to control the logging level of output.
	 *
	 * @param level
	 * 		Level for this filter instance.
	 */
	public RecafLoggingFilter(@Nullable Level level) {
		instanceLevel = () -> Objects.requireNonNullElse(level, Level.TRACE);
	}

	@Override
	public FilterReply decide(@Nonnull ILoggingEvent event) {
		Level level = event.getLevel();
		if (instanceLevel.get().isGreaterOrEqual(level))
			return FilterReply.DENY;
		String loggerName = event.getLoggerName();
		if (loggerName.startsWith("software.coley.") || Logging.loggerKeys().contains(loggerName))
			return FilterReply.ACCEPT;
		return FilterReply.DENY;
	}
}