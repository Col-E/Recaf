package software.coley.recaf.analytics.logging;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;

import java.lang.System.Logger.Level;

/**
 * Logging policy that only allows Recaf logger calls.
 *
 * @author Matt Coley
 */
@ExcludeFromJacocoGeneratedReport(justification = "Logging not relevant for test coverage")
public final class RecafLoggingFilter {
	private static volatile Level consoleLevel = Level.DEBUG;

	private RecafLoggingFilter() {}

	/**
	 * @return Console logging threshold.
	 */
	@Nonnull
	public static Level getConsoleLevel() {
		return consoleLevel;
	}

	/**
	 * @param level
	 * 		New console logging threshold.
	 */
	public static void setConsoleLevel(@Nonnull Level level) {
		consoleLevel = level;
	}

	/**
	 * @param loggerName
	 * 		Logger name.
	 * @param level
	 * 		Logging level.
	 *
	 * @return {@code true} when the console should emit the log event.
	 */
	public static boolean allowsConsole(@Nonnull String loggerName, @Nonnull Level level) {
		return consoleLevel != Level.OFF &&
				level != Level.OFF &&
				isRecafLogger(loggerName) &&
				level.getSeverity() >= consoleLevel.getSeverity();
	}

	/**
	 * @param loggerName
	 * 		Logger name.
	 * @param level
	 * 		Logging level.
	 *
	 * @return {@code true} when the file sink should emit the log event.
	 */
	public static boolean allowsFile(@Nonnull String loggerName, @Nonnull Level level) {
		return level != Level.OFF && isRecafLogger(loggerName);
	}

	private static boolean isRecafLogger(@Nonnull String loggerName) {
		return loggerName.startsWith("software.coley.") || Logging.loggerKeys().contains(loggerName);
	}
}
